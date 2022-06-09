package io.kyligence.kap.gateway.filter;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.exception.ForbiddenException;
import io.kyligence.kap.gateway.loadbalancer.KylinLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.msg.ErrorCode;
import org.springframework.cloud.gateway.support.msg.MsgPicker;
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

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PROJECT_KEY;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PROJECT_NO_RESOURCE_GROUP_EXCEPTION;

@Slf4j
public class KylinLoadBalancerClientFilter extends LoadBalancerClientFilter
		implements ApplicationListener<RefreshRoutesEvent> {

	private Map<String, KylinLoadBalancer> resourceGroups = new ConcurrentHashMap<>();

	public KylinLoadBalancerClientFilter(LoadBalancerClient loadBalancer,
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

		if (Boolean.valueOf(exchange.getAttribute(PROJECT_NO_RESOURCE_GROUP_EXCEPTION))
				&& StringUtils.isNotBlank(exchange.getAttribute(PROJECT_KEY))) {
			throw ForbiddenException.create(
					MsgPicker.getMsg().getContext(exchange.getAttribute(PROJECT_KEY), ErrorCode.PROJECT_NO_RESOURCE_GROUP));
		}

		return choose(uri.getAuthority(), null);
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
				KylinLoadBalancer kylinLoadBalancer = ((KylinLoadBalancer) resourceGroup);
				newResourceGroups.put(kylinLoadBalancer.getServiceId(), kylinLoadBalancer);
			}
		});

		Collection<KylinLoadBalancer> oldResourceGroups = resourceGroups.values();
		resourceGroups = newResourceGroups;
		oldResourceGroups.stream().filter(lb -> lb.getMvcc() < mvcc).forEach(KylinLoadBalancer::shutdown);

		for (KylinLoadBalancer loadBalancer : newResourceGroups.values()) {
			log.info("Saved LoadBalancer: {}", loadBalancer);
		}
	}

	@Override
	public void addResourceGroups(List<BaseLoadBalancer> addResourceGroups) {
		addResourceGroups.forEach(resourceGroup -> {
			if (resourceGroup instanceof KylinLoadBalancer) {
				KylinLoadBalancer kylinLoadBalancer = ((KylinLoadBalancer) resourceGroup);
				resourceGroups.putIfAbsent(kylinLoadBalancer.getServiceId(), kylinLoadBalancer);
			}
		});
	}

	@Override
	public Map<String, Object> getLoadBalancerServers() {
		return resourceGroups.values().stream().collect(Collectors.toMap(KylinLoadBalancer::getServiceId, value -> Arrays.toString(value.getAllServers().toArray())));
	}

}
