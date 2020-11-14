package io.kyligence.kap.gateway.entity;

import com.google.common.collect.Lists;
import com.netflix.loadbalancer.BaseLoadBalancer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KylinRouteTable {

	private boolean broken = false;

	private long mvcc = -1;

	private final List<Route> routes = Lists.newArrayList();

	public void addRoute(Route route) {
		routes.add(route);
	}

	public List<RouteDefinition> getRouteDefinitionList() {
		return routes.stream().map(Route::getRouteDefinition).collect(Collectors.toList());
	}

	public List<BaseLoadBalancer> getLoadBalancerList() {
		return routes.stream().map(Route::getLoadBalancer).collect(Collectors.toList());
	}

	@Getter
	@AllArgsConstructor
	public static class Route {
		private RouteDefinition routeDefinition;
		private BaseLoadBalancer loadBalancer;
	}
}
