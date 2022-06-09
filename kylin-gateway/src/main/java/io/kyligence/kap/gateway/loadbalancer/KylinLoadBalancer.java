package io.kyligence.kap.gateway.loadbalancer;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.LoadBalancerStats;
import io.kyligence.kap.gateway.loadbalancer.ConcurrentPingStrategy;

public class KylinLoadBalancer extends BaseLoadBalancer {

	private static final int DEFAULT_INTERVAL_SECONDS = 1;
	private long mvcc = 0;
	private boolean broken = false;

	public KylinLoadBalancer(String name, IPing ping, IRule rule, IPingStrategy pingStrategy, long mvcc) {
		super(name, rule, new LoadBalancerStats(name), null, pingStrategy);
		if (pingStrategy instanceof ConcurrentPingStrategy) {
			setPingInterval(DEFAULT_INTERVAL_SECONDS);
		}

		setPing(ping);
		this.mvcc = mvcc;
	}

	@Override
	public void forceQuickPing() {
		super.forceQuickPing();
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

	@Override
	public String toString() {
		return super.toString();
	}
}
