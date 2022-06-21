package io.kyligence.kap.gateway.loadbalancer;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.constant.LoadBalancerStrategy;
import io.kyligence.kap.gateway.manager.MdxLoadManager;

import java.nio.charset.Charset;
import java.util.List;

public class MdxLoadBalancer extends BaseLoadBalancer {

	private static final int DEFAULT_INTERVAL_SECONDS = 3;

	private final long mvcc;

	private boolean broken = false;

	private String strategy;

	public MdxLoadBalancer(String name, IPing ping, IRule rule, IPingStrategy pingStrategy, long mvcc) {
		super(name, rule, new LoadBalancerStats(name), null, pingStrategy);
		this.mvcc = mvcc;
		if (pingStrategy instanceof ConcurrentPingStrategy) {
			setPingInterval(DEFAULT_INTERVAL_SECONDS);
		}
		setPing(ping);
	}

	@Override
	public Server chooseServer(Object serviceKey) {
		String strategy = getStrategy();
		if (LoadBalancerStrategy.CONSISTENT_HASH.name().equalsIgnoreCase(strategy)) {
			// consistent hash strategy
			List<Server> servers = getReachableServers();
			HashCode hashCode = Hashing.murmur3_128().hashString(serviceKey.toString(), Charset.defaultCharset());
			int index = Hashing.consistentHash(hashCode, servers.size());
			return servers.get(index);
		} else {
			// choose node server by cluster server load
			List<Server> serverList = getReachableServers();
			Server chooseServer = null;
			double minLoad = Double.MAX_VALUE;
			for (Server server : serverList) {
				MdxLoadManager.LoadInfo loadInfo = MdxLoadManager.LOAD_INFO_MAP.get(server.getId());
				if (loadInfo == null || loadInfo.getNodeLoad() == null) {
					continue;
				}
				double nodeLoad = loadInfo.getNodeLoad();
				if (nodeLoad < minLoad) {
					chooseServer = server;
					minLoad = nodeLoad;
				}
			}
			if (chooseServer != null) {
				return chooseServer;
			}
		}
		// Use 'default' on a null hint, or just pass it on
		return super.chooseServer(serviceKey != null ? serviceKey : "default");
	}

	public String getServiceId() {
		return getName();
	}

	public long getMvcc() {
		return mvcc;
	}

	public boolean isBroken() {
		return broken;
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	@Override
	public String toString() {
		return "{NFLoadBalancer:name=" + this.getName() +
				",all servers=" + this.allServerList +
				",up servers=" + this.upServerList.toString() +
				",lb stats=" + this.lbStats.toString() + "}";
	}

}
