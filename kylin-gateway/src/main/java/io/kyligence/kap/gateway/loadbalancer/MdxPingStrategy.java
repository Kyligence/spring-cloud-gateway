package io.kyligence.kap.gateway.loadbalancer;

import com.google.common.collect.Lists;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.config.MdxConfig;
import io.kyligence.kap.gateway.event.KylinRefreshRoutesEvent;
import io.kyligence.kap.gateway.manager.MdxLoadManager;
import io.kyligence.kap.gateway.manager.task.GenerateSchemaTask;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * @author liang.xu
 */
@Slf4j
@Data
public class MdxPingStrategy implements IPingStrategy, ApplicationListener<KylinRefreshRoutesEvent> {

	private final ScheduledExecutorService pingRefresher;
	private Map<Server, AtomicInteger> serversStatus = new ConcurrentHashMap<>();
	private ExecutorService executorService = Executors.newCachedThreadPool();

	private int retryTimes;

	private int intervalSeconds;

	private int generateSchemaIntervalSeconds;

	private boolean generateSchema;

	@Autowired
	private MdxConfig mdxConfig;

	@Autowired
	private MdxLoadManager mdxLoadManager;

	private IPing ping;

	public MdxPingStrategy(IPing ping) {
		this.ping = ping;
		this.pingRefresher = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("PingRefresher"));
	}

	@PostConstruct
	public void init() {
		if (generateSchema) {
			pingRefresher.scheduleWithFixedDelay(this::generateSchema, 0, generateSchemaIntervalSeconds, TimeUnit.SECONDS);
		}
		pingRefresher.scheduleWithFixedDelay(this::pingServers, 10, intervalSeconds > 0 ? intervalSeconds : 3, TimeUnit.SECONDS);
	}

	@Override
	public synchronized void onApplicationEvent(KylinRefreshRoutesEvent event) {
		for (Server server : serversStatus.keySet()) {
			if (serversStatus.get(server).get() > retryTimes) {
				mdxLoadManager.removeServer(server.getId());
			}
		}
		Set<Server> removeList = serversStatus.keySet().stream().filter(server -> !event.getServerSet().contains(server)).collect(Collectors.toSet());
		removeList.forEach(serversStatus::remove);
	}

	@Override
	public boolean[] pingServers(IPing ping, Server[] servers) {
		if (ArrayUtils.isEmpty(servers)) {
			log.debug("Ping servers is empty!");
			return new boolean[]{false};
		}

		boolean[] results = new boolean[servers.length];

		List<Server> notCachedServers = Lists.newArrayList();
		for (int i = 0; i < servers.length; i++) {
			if (serversStatus.containsKey(servers[i])) {
				AtomicInteger errorTimes = serversStatus.get(servers[i]);
				results[i] = Objects.nonNull(errorTimes) && errorTimes.get() < retryTimes;
			} else {
				notCachedServers.add(servers[i]);
			}
		}

		if (notCachedServers.isEmpty()) {
			return results;
		}

		synchronized (MdxPingStrategy.class) {
			pingServers(notCachedServers);

			for (int i = 0; i < servers.length; i++) {
				AtomicInteger errorTimes = serversStatus.get(servers[i]);
				results[i] = Objects.nonNull(errorTimes) && errorTimes.get() < retryTimes;
			}
		}

		return results;
	}

	private void pingServers() {
		try {
			pingServers(Lists.newArrayList(serversStatus.keySet()));
		} catch (Exception e) {
			log.error("Failed to run cron ping servers", e);
		}
	}

	public synchronized boolean[] pingServers(List<Server> servers) {
		if (CollectionUtils.isEmpty(servers)) {
			return new boolean[]{false};
		}

		boolean[] results = new boolean[servers.size()];

		Future<Boolean>[] futures = new Future[servers.size()];

		for (int i = 0; i < servers.size(); i++) {
			futures[i] = executorService.submit(new CheckServerTask(this.ping, servers.get(i), mdxLoadManager));
		}

		for (int i = 0; i < servers.size(); i++) {
			try {
				results[i] = futures[i].get();
			} catch (Exception e) {
				log.error("Task execute failed, server: {}", servers.get(i));
			}
		}
		return results;
	}

	private void generateSchema() {
		try {
			List<String> serverList = new LinkedList<>();
			for (MdxConfig.ProxyInfo proxyInfo : mdxConfig.getProxy()) {
				serverList.addAll(proxyInfo.getServers());
			}
			List<Server> serverList1 = serverList.stream().map(Server::new).collect(Collectors.toList());
			generateAllServerSchema(serverList1);
		} catch (Exception e) {
			log.error("Failed to run cron generate schema", e);
		}
	}

	public void generateAllServerSchema(List<Server> servers) {
		if (CollectionUtils.isEmpty(servers)) {
			return;
		}
		for (Server server : servers) {
			executorService.submit(new GenerateSchemaTask(this.ping, server));
		}
	}

	private class CheckServerTask implements Callable<Boolean> {

		private final IPing ping;

		private final Server server;

		private final MdxLoadManager mdxLoadManager;

		private CheckServerTask(IPing ping, Server server, MdxLoadManager mdxLoadManager) {
			this.ping = ping;
			this.server = server;
			this.mdxLoadManager = mdxLoadManager;
		}

		@Override
		public Boolean call() {
			serversStatus.putIfAbsent(server, new AtomicInteger(retryTimes));
			AtomicInteger errorTimes = serversStatus.get(server);

			try {
				if (ping instanceof MdxPing) {
					switch (((MdxPing) ping).checkServer(server)) {
						case NORMAL:
							errorTimes.set(0);
							double load = ((MdxPing) ping).getServerLoad(server);
							mdxLoadManager.updateServerByMemLoad(server.getId(), load);
							return true;
						case FATAL:
							// stop route immediately
							if (errorTimes.get() < retryTimes - 1) {
								errorTimes.set(retryTimes - 1);
							}
							break;
						case ERROR:
							// check 2 times
							if (errorTimes.get() < retryTimes - 2) {
								errorTimes.set(retryTimes - 2);
							}
							break;
						case WARN:
						default:
							break;
					}
				} else if (ping.isAlive(server)) {
					errorTimes.set(0);
					return true;
				}
			} catch (Exception e) {
				log.error("Failed to ping server: {}", server, e);
			}

			return errorTimes.incrementAndGet() < retryTimes;
		}
	}

}
