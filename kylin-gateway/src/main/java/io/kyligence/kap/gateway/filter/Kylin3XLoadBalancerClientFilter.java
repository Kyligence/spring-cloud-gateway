package io.kyligence.kap.gateway.filter;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.utils.AsyncQueryUtil;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.context.ApplicationListener;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.kyligence.kap.gateway.constant.KylinRouteConstant.ASYNC_QUERY_SUFFIX;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.DEFAULT_RESOURCE_GROUP;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.QUERY_SUFFIX;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

public class Kylin3XLoadBalancerClientFilter extends LoadBalancerClientFilter
		implements ApplicationListener<RefreshRoutesEvent> {

	private Map<String, KylinLoadBalancer> resourceGroups = new ConcurrentHashMap<>();

	public Kylin3XLoadBalancerClientFilter(LoadBalancerClient loadBalancer,
										   LoadBalancerProperties properties) {
		super(loadBalancer, properties);
	}

	public ILoadBalancer getLoadBalancer(String serviceId) {
		return resourceGroups.get(serviceId);
	}

	protected Server getServer(ILoadBalancer loadBalancer, Object hint) {
		if (loadBalancer == null) {
			return null;
		}
		// Use 'default' on a null hint, or just pass it on?
		return loadBalancer.chooseServer(hint != null ? hint : "default");
	}

	public ServiceInstance choose(String serviceId, Object hint) {
		Server server = getServer(getLoadBalancer(serviceId), hint);
		if (server == null) {
			return null;
		}
		return new RibbonLoadBalancerClient.RibbonServer(serviceId, server);
	}

	@Override
	protected ServiceInstance choose(ServerWebExchange exchange) {
		URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
		if (null == uri) {
			return null;
		}

		ServiceInstance serviceInstance = null;
		if (isQueryRequest(uri)) {
			if (isRoute2Default(uri)) {
				if (!properties.isQueryRouteDefault()) {
					return null;
				}
			} else {
				if (isAsyncQueryRequest(uri)) {
					serviceInstance = choose(
							AsyncQueryUtil.buildAsyncQueryServiceId(uri.getAuthority()),
							AsyncQueryUtil.ASYNC_QUERY_SUFFIX_TAG);
				}
			}
		}

		return serviceInstance != null ? serviceInstance : choose(uri.getAuthority(), null);
	}

	private boolean isAsyncQueryRequest(URI uri) {
		return uri.getPath().endsWith(ASYNC_QUERY_SUFFIX);
	}

	private boolean isQueryRequest(URI uri) {
		return uri.getPath().endsWith(QUERY_SUFFIX) || isAsyncQueryRequest(uri);
	}

	private boolean isRoute2Default(URI uri) {
		return DEFAULT_RESOURCE_GROUP.equals(uri.getAuthority());
	}

	@Override
	public void onApplicationEvent(RefreshRoutesEvent event) {
		// Nothing to do
	}

	@Override
	public void updateResourceGroups(List<BaseLoadBalancer> updateResourceGroups, final long mvcc) {
		ConcurrentHashMap<String, KylinLoadBalancer> newResourceGroups = new ConcurrentHashMap<>();

		updateResourceGroups.forEach(resourceGroup -> {
			if (resourceGroup instanceof KylinLoadBalancer) {
				KylinLoadBalancer kylin3XLoadBalancer = ((KylinLoadBalancer) resourceGroup);
				newResourceGroups.put(kylin3XLoadBalancer.getServiceId(), kylin3XLoadBalancer);
			}
		});

		Collection<KylinLoadBalancer> oldResourceGroups = resourceGroups.values();
		resourceGroups = newResourceGroups;
		oldResourceGroups.stream().filter(lb -> lb.getMvcc() < mvcc).forEach(KylinLoadBalancer::shutdown);
	}

	@Override
	public void addResourceGroups(List<BaseLoadBalancer> addResourceGroups) {
		addResourceGroups.forEach(resourceGroup -> {
			if (resourceGroup instanceof KylinLoadBalancer) {
				KylinLoadBalancer kylin3XLoadBalancer = ((KylinLoadBalancer) resourceGroup);
				resourceGroups.putIfAbsent(kylin3XLoadBalancer.getServiceId(),
						kylin3XLoadBalancer);
			}
		});
	}

	@Override
	public Map<String, Object> getLoadBalancerServers() {
		return resourceGroups.values().stream().collect(Collectors.toMap(KylinLoadBalancer::getServiceId, value -> Arrays.toString(value.getAllServers().toArray())));
	}
}
