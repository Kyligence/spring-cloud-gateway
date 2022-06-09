package io.kyligence.kap.gateway.manager;

import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.bean.ServerInfo;
import io.kyligence.kap.gateway.utils.TimeUtil;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceManager {

	private static final int IP_PORT_LENGTH = 2;

	/**
	 * server cache
	 * key: BI request: user_project, normal request: host
	 * value: ServerInfo, contains: server ip:port, cache start time and cache update time
	 */
	public final Map<String, ServerInfo> serverMap = new ConcurrentHashMap<>();

	public ServiceInstance getServiceInstance(String hostName, String serverKey) {
		ServerInfo serverInfo = serverMap.get(serverKey);
		if (serverInfo == null || serverInfo.getServer() == null) {
			return null;
		}
		String[] ipPort = serverInfo.getServer().split(":");
		if (ipPort.length != IP_PORT_LENGTH) {
			return null;
		}
		Server server = new Server(ipPort[0], Integer.parseInt(ipPort[1]));
		ServerInfo newServerInfo = new ServerInfo(serverInfo.getServer(), serverInfo.getStartTime(), TimeUtil.getSecondTime());
		serverMap.put(serverKey, newServerInfo);
		return new RibbonLoadBalancerClient.RibbonServer(hostName, server);
	}

	public void putServiceInstance(String serviceKey, ServerInfo serverInfo) {
		serverMap.put(serviceKey, serverInfo);
	}

}
