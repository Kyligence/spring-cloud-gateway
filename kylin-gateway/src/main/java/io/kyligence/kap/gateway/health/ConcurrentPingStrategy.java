package io.kyligence.kap.gateway.health;

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.Server;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
public class ConcurrentPingStrategy implements IPingStrategy {

	private Map<Server, AtomicInteger> failCounter = new ConcurrentHashMap<>();

	private ExecutorService executorService;

	private int retryTimes;

	private int intervalSeconds;

	@Override
	public boolean[] pingServers(IPing ping, Server[] servers) {
		int numCandidates = servers.length;
		boolean[] results = new boolean[numCandidates];
		if (Objects.isNull(ping)) {
			log.error("Ping object is null");
			return results;
		}

		executorService = Executors.newCachedThreadPool();

		Future[] futures = new Future[numCandidates];

		for (int i = 0; i < numCandidates; i++) {
			futures[i] = executorService.submit(new CheckServerTask(ping, servers[i]));
		}

		for (int i = 0; i < numCandidates; i++) {
			try {
				results[i] = (Boolean) futures[i].get();
			}
			catch (Exception e) {
				log.error("Task execute failed, server: {}", servers[i]);
			}
		}

		executorService.shutdown();

		return results;
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
			boolean result;
			try {
				boolean isAlive = ping.isAlive(server);
				if (isAlive) {
					failCounter.remove(server);
					return true;
				}
			}
			catch (Exception e) {
				log.error("Exception while pinging Server: '{}'", server, e);
			}

			if (failCounter.containsKey(server)) {
				failCounter.get(server).addAndGet(1);
			}
			else {
				failCounter.put(server, new AtomicInteger(1));
			}

			result = failCounter.get(server).get() < retryTimes;

			return result;
		}

	}

}
