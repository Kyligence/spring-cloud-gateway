package io.kyligence.kap.gateway.route.transformer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.RoundRobinRule;
import io.kyligence.kap.gateway.config.GlobalConfig;
import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.entity.KylinRouteTable;
import io.kyligence.kap.gateway.loadbalancer.MdxLoadBalancer;
import io.kyligence.kap.gateway.loadbalancer.MdxPing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static io.kyligence.kap.gateway.constant.KylinRouteConstant.KYLIN_GLOBAL_ROUTE_PREDICATE;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.PREDICATE_ARG_KEY_0;

@Slf4j
@ConditionalOnProperty(name = "server.type", havingValue = KylinGatewayVersion.MDX)
@Component
public class MdxRouteTableTransformer implements RouteTableTransformer {

	@Autowired
	private GlobalConfig globalConfig;

	@Autowired
	private MdxPing ping;

	@Autowired
	private IPingStrategy pingStrategy;

	private String getStringURIByServiceId(String serviceId) {
		return "lb://" + serviceId;
	}

	@Override
	public RouteDefinition convert2RouteDefinition(KylinRouteRaw rawRoute) throws URISyntaxException {
		RouteDefinition routeDefinition = new RouteDefinition();
		String uuid = String.valueOf(rawRoute.getId());
		routeDefinition.setId(uuid);
		routeDefinition.setUri(new URI(getStringURIByServiceId(rawRoute.getHost())));
		PredicateDefinition predicateDefinition = new PredicateDefinition();
		predicateDefinition.setName(KYLIN_GLOBAL_ROUTE_PREDICATE);
		predicateDefinition.getArgs().put(PREDICATE_ARG_KEY_0, "/**");
		routeDefinition.setPredicates(Lists.newArrayList(predicateDefinition));
		routeDefinition.setOrder(rawRoute.getOrder());
		return routeDefinition;
	}

	@Override
	public MdxLoadBalancer convert2KylinLoadBalancer(KylinRouteRaw routeRaw) {
		MdxLoadBalancer mdxLoadBalancer =
				new MdxLoadBalancer(routeRaw.getHost(), ping, new RoundRobinRule(), pingStrategy, globalConfig.getLastValidRawRouteTableMvcc().get());
		mdxLoadBalancer.setStrategy(routeRaw.getStrategy());
		mdxLoadBalancer.addServers(routeRaw.getBackends());
		return mdxLoadBalancer;
	}

	@Override
	public KylinRouteTable convert(List<KylinRouteRaw> rawRouteTable) {
		Preconditions.checkArgument(CollectionUtils.isNotEmpty(rawRouteTable));

		KylinRouteTable routeTable = new KylinRouteTable();
		routeTable.setMvcc(globalConfig.getLastValidRawRouteTableMvcc().get());

		Map<String, BaseLoadBalancer> loadBalancerMap = Maps.newHashMap();
		for (KylinRouteRaw rawRoute : rawRouteTable) {
			try {
				RouteDefinition routeDefinition = convert2RouteDefinition(rawRoute);
				if (routeDefinition.isBroken()) {
					continue;
				}

				MdxLoadBalancer loadBalancer;
				String serviceId = rawRoute.getHost();
				if (loadBalancerMap.containsKey(serviceId)) {
					if (!CollectionUtils.isEqualCollection(loadBalancerMap.get(serviceId).getAllServers(), rawRoute.getBackends())) {
						throw new RuntimeException("Same resource group, make difference server list !");
					}

					loadBalancer = (MdxLoadBalancer) loadBalancerMap.get(serviceId);
				} else {
					loadBalancer = convert2KylinLoadBalancer(rawRoute);
					if (loadBalancer.isBroken()) {
						loadBalancer.shutdown();
						continue;
					}
				}

				loadBalancerMap.put(serviceId, loadBalancer);

				routeTable.addRoute(new KylinRouteTable.Route(routeDefinition, loadBalancer));
			} catch (Exception e) {
				log.error("Failed to convert rawRoute to KylinRouteTable, rawRoute: {}", rawRoute, e);
				routeTable.setBroken(true);
			}
		}

		return routeTable;
	}

}
