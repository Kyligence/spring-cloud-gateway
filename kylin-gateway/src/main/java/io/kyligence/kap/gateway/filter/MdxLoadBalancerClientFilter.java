package io.kyligence.kap.gateway.filter;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.bean.ServerInfo;
import io.kyligence.kap.gateway.loadbalancer.MdxLoadBalancer;
import io.kyligence.kap.gateway.manager.MdxLoadManager;
import io.kyligence.kap.gateway.manager.ServiceManager;
import io.kyligence.kap.gateway.utils.MdxAuthenticationUtils;
import io.kyligence.kap.gateway.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MdxLoadBalancerClientFilter extends LoadBalancerClientFilter
		implements ApplicationListener<RefreshRoutesEvent> {

	private static final String X_HOST = "X-Host";
	private static final String X_PORT = "X-Port";
	private static final String DEFAULT_KEY = "default";

	private Map<String, MdxLoadBalancer> resourceGroups = new ConcurrentHashMap<>();

	private final RemoteAddressResolver remoteAddressResolver = XForwardedRemoteAddressResolver.trustAll();

	private final ServiceManager serviceManager;

	private final MdxLoadManager mdxLoadManager;

	public MdxLoadBalancerClientFilter(LoadBalancerClient loadBalancer, LoadBalancerProperties properties,
									   ServiceManager serviceManager, MdxLoadManager mdxLoadManager) {
		super(loadBalancer, properties);
		this.serviceManager = serviceManager;
		this.mdxLoadManager = mdxLoadManager;
	}

	public ILoadBalancer getLoadBalancer(String serviceId) {
		return resourceGroups.get(serviceId);
	}

	@Override
	public void onApplicationEvent(RefreshRoutesEvent event) {
		// Nothing to do
	}

	@Override
	protected ServiceInstance choose(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders httpHeaders = request.getHeaders();
		String hostName = "";
		List<String> hosts = httpHeaders.get(HttpHeaders.HOST);
		if (CollectionUtils.isNotEmpty(hosts)) {
			hostName = hosts.get(0);
		}
		String[] tmp = hostName.split(":");
		if (tmp.length < 2) {
			hostName = tmp[0] + ":80";
		}

		// Diagnostic pack request, routed by X-Host and X-Port
		if (httpHeaders.get(X_HOST) != null && httpHeaders.get(X_PORT) != null) {
			String xHost = httpHeaders.get(X_HOST).get(0);
			String xPort = httpHeaders.get(X_PORT).get(0);
			if (StringUtils.isNotBlank(xHost) && StringUtils.isNotBlank(xPort)) {
				Server server = new Server(xHost, Integer.parseInt(xPort));
				mdxLoadManager.updateServerByQueryNum(server.getId(), 1);
				return new RibbonLoadBalancerClient.RibbonServer(hostName, server);
			}
		}

		String username = null;
		String project = null;
		try {
			username = MdxAuthenticationUtils.getUsername(request);
			project = MdxAuthenticationUtils.getProject(request.getPath().toString());
		} catch (Exception ignored) {
		}

		String serverKey;
		if (StringUtils.isNotBlank(username)) {
			// BI request, routed by user and project
			if (StringUtils.isBlank(project)) {
				project = DEFAULT_KEY;
			}
			serverKey = username.toUpperCase() + "_" + project;
		} else {
			// Normal request, routed by request host
			InetSocketAddress remoteAddress = remoteAddressResolver.resolve(exchange);
			if (remoteAddress != null) {
				serverKey = remoteAddress.getHostString();
			} else {
				serverKey = DEFAULT_KEY;
			}
		}

		ServiceInstance serviceInstance = serviceManager.getServiceInstance(hostName, serverKey);
		if (serviceInstance != null) {
			mdxLoadManager.updateServerByQueryNum(serviceInstance.getUri().getAuthority(), 1);
			return serviceInstance;
		}
		return choose(hostName, serverKey);
	}

	@Override
	public void updateResourceGroups(List<BaseLoadBalancer> updateResourceGroups, final long mvcc) {
		ConcurrentHashMap<String, MdxLoadBalancer> newResourceGroups = new ConcurrentHashMap<>();

		updateResourceGroups.forEach(resourceGroup -> {
			if (resourceGroup instanceof MdxLoadBalancer) {
				MdxLoadBalancer mdxLoadBalancer = ((MdxLoadBalancer) resourceGroup);
				newResourceGroups.put(mdxLoadBalancer.getServiceId(), mdxLoadBalancer);
			}
		});

		Collection<MdxLoadBalancer> oldResourceGroups = resourceGroups.values();
		resourceGroups = newResourceGroups;
		oldResourceGroups.stream().filter(lb -> lb.getMvcc() < mvcc).forEach(MdxLoadBalancer::shutdown);

		for (MdxLoadBalancer loadBalancer : newResourceGroups.values()) {
			log.info("Saved LoadBalancer: {}", loadBalancer);
		}
	}

	@Override
	public void addResourceGroups(List<BaseLoadBalancer> addResourceGroups) {
		addResourceGroups.forEach(resourceGroup -> {
			if (resourceGroup instanceof MdxLoadBalancer) {
				MdxLoadBalancer mdxLoadBalancer = ((MdxLoadBalancer) resourceGroup);
				resourceGroups.putIfAbsent(mdxLoadBalancer.getServiceId(), mdxLoadBalancer);
			}
		});
	}

	@Override
	public Map<String, Object> getLoadBalancerServers() {
		return resourceGroups.values().stream().collect(Collectors.toMap(MdxLoadBalancer::getServiceId, value -> Arrays.toString(value.getAllServers().toArray())));
	}

	private ServiceInstance choose(String serviceId, String serverKey) {
		Server server = getServer(serviceId, serverKey);
		if (server == null) {
			log.warn("Host: {}, 503, Service Unavailable!", serviceId);
			return null;
		}
		mdxLoadManager.updateServerByQueryNum(server.getId(), 1);
		ServerInfo serverInfo = new ServerInfo(server.getId(), TimeUtil.getSecondTime(), TimeUtil.getSecondTime());
		serviceManager.putServiceInstance(serverKey, serverInfo);
		return new RibbonLoadBalancerClient.RibbonServer(serviceId, server);
	}

	private Server getServer(String serviceId, String serviceKey) {
		ILoadBalancer loadBalancer = getLoadBalancer(serviceId);
		if (loadBalancer == null) {
			return null;
		}
		return loadBalancer.chooseServer(serviceKey);
	}

}
