package io.kyligence.kap.gateway.filter;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.LoadBalancerStats;
import io.kyligence.kap.gateway.health.ConcurrentPingStrategy;

public class Kylin3XLoadBalancer extends BaseLoadBalancer {

	public Kylin3XLoadBalancer(String name, IPing ping, IRule rule, IPingStrategy pingStrategy) {
		super(name, rule, new LoadBalancerStats(name), ping, pingStrategy);
		if (pingStrategy instanceof ConcurrentPingStrategy) {
			setPingInterval(((ConcurrentPingStrategy) pingStrategy).getIntervalSeconds());
		}
	}

	public String getServiceId() {
		return getName();
	}

}
