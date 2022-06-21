package io.kyligence.kap.gateway.manager;

import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.bean.ServerInfo;
import io.kyligence.kap.gateway.manager.task.ClearExpiredServerTask;
import io.kyligence.kap.gateway.utils.TimeUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ServiceManager {

	private static final int IP_PORT_LENGTH = 2;

	/**
	 * server cache
	 * key: BI request: user_project, normal request: host or ip
	 * value: ServerInfo, contains: server ip:port, cache start time and cache update time
	 */
	@Getter
	private final Map<String, ServerInfo> serverMap = new ConcurrentHashMap<>();

	private final ScheduledExecutorService scheduledExecService = new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory("schedule-server"));

	@PostConstruct
	public void init() {
		scheduledExecService.scheduleWithFixedDelay(
				new ClearExpiredServerTask(serverMap),
				30,
				30,
				TimeUnit.SECONDS
		);
	}

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
