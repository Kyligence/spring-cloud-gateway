package io.kyligence.kap.gateway.health;

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.event.KylinRefreshRoutesEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhiyu.zeng
 */

@Slf4j
@Data
public class ConcurrentPingStrategy implements IPingStrategy, ApplicationListener<KylinRefreshRoutesEvent> {

	private Map<Server, AtomicInteger> serversStatus = new ConcurrentHashMap<>();

	private ExecutorService executorService = Executors.newCachedThreadPool();

	private int retryTimes;

	private int intervalSeconds;

	private boolean[] initTrueArray(boolean[] array) {
		for (int i = 0; i < array.length; i++) {
			array[i] = true;
		}
		return array;
	}

	@Override
	public boolean[] pingServers(IPing ping, Server[] servers) {
		if (servers.length < 1) {
			log.debug("Ping servers is empty!");
			return new boolean[]{false};
		}

		boolean[] results = initTrueArray(new boolean[servers.length]);
		if (Objects.isNull(ping)) {
			log.error("Ping object is null!");
			return results;
		}

		Future[] futures = new Future[servers.length];

		for (int i = 0; i < servers.length; i++) {
			futures[i] = executorService.submit(new CheckServerTask(ping, servers[i]));
		}

		for (int i = 0; i < servers.length; i++) {
			try {
				results[i] = (Boolean) futures[i].get();
			} catch (Exception e) {
				log.error("Task execute failed, server: {}", servers[i]);
			}
		}

		return results;
	}

	@Override
	public void onApplicationEvent(KylinRefreshRoutesEvent event) {
		serversStatus.clear();
	}

	private class CheckServerTask implements Callable<Boolean> {

		private IPing ping;

		private Server server;

		private CheckServerTask(IPing ping, Server server) {
			this.ping = ping;
			this.server = server;
		}

		@Override
		public Boolean call() {
			serversStatus.putIfAbsent(server, new AtomicInteger(0));
			AtomicInteger errorTimes = serversStatus.get(server);
			if (null == errorTimes) {
				// for clear servers
				return false;
			}

			try {
				boolean isAlive = ping.isAlive(server);
				if (isAlive) {
					if (errorTimes.get() > 0) {
						errorTimes.set(0);
					}
					return true;
				}
			} catch (Exception e) {
				log.error("Failed to ping server: {}", server, e);
			}

			return errorTimes.incrementAndGet() < retryTimes;
		}
	}

}
