package io.kyligence.kap.gateway.filter;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.LoadBalancerStats;
import io.kyligence.kap.gateway.health.ConcurrentPingStrategy;
import io.micrometer.core.instrument.util.NamedThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class KylinLoadBalancer extends BaseLoadBalancer {

	public static final ThreadPoolExecutor threadPoolExecutor =
			new ThreadPoolExecutor(1, 4, 0, TimeUnit.SECONDS,
					new LinkedBlockingQueue<>(), new NamedThreadFactory("LoadBalancer-PingTask"));

	private long mvcc = 0;

	private boolean broken = false;

	public KylinLoadBalancer(String name, IPing ping, IRule rule, IPingStrategy pingStrategy, long mvcc) {
		super(name, rule, new LoadBalancerStats(name), null, pingStrategy);
		if (pingStrategy instanceof ConcurrentPingStrategy) {
			setPingInterval(((ConcurrentPingStrategy) pingStrategy).getIntervalSeconds());
		}

		setPing(ping);
		this.mvcc = mvcc;
	}

	@Override
	public void forceQuickPing() {
		threadPoolExecutor.submit(super::forceQuickPing);
	}

	public String getServiceId() {
		return getName();
	}


	public long getMvcc() {
		return mvcc;
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	public boolean isBroken() {
		return broken;
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
