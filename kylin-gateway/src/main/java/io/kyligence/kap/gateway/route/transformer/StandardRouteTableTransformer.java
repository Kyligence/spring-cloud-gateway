package io.kyligence.kap.gateway.route.transformer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.RoundRobinRule;
import io.kyligence.kap.gateway.config.GlobalConfig;
import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import io.kyligence.kap.gateway.constant.KylinResourceGroupTypeEnum;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.entity.KylinRouteTable;
import io.kyligence.kap.gateway.filter.KylinLoadBalancer;
import io.kyligence.kap.gateway.health.KylinPing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.kyligence.kap.gateway.constant.KylinResourceGroupTypeEnum.GLOBAL;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.DEFAULT_RESOURCE_GROUP;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.GLOBAL_RESOURCE_GROUP;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.KYLIN_ROUTE_PREDICATE;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.PREDICATE_ARG_KEY_0;

@Slf4j
@ConditionalOnProperty(name = "kylin.gateway.ke.version", havingValue = KylinGatewayVersion.KYLIN_4X)
@Component
public class StandardRouteTableTransformer implements RouteTableTransformer {

	@Autowired
	private GlobalConfig globalConfig;

	@Autowired
	private KylinPing ping;

	@Autowired
	private IPingStrategy pingStrategy;

	private String resourceGroup2ServiceId(String resourceGroup) {
		Preconditions.checkArgument(StringUtils.isNotBlank(resourceGroup));

		return UUID.nameUUIDFromBytes(resourceGroup.getBytes()).toString().substring(0, 9) + resourceGroup.replace('_', '-');
	}

	private String getServiceId(KylinRouteRaw routeRaw) {
		String serviceId;
		switch (KylinResourceGroupTypeEnum.valueOf(routeRaw.getType())) {
			case GLOBAL:
				serviceId = GLOBAL_RESOURCE_GROUP;
				break;
			case DEFAULT:
				serviceId = DEFAULT_RESOURCE_GROUP;
				break;
			default:
				serviceId = resourceGroup2ServiceId(routeRaw.getResourceGroup());
				break;
		}

		return serviceId;
	}

	private String getStringURIByServiceId(String serviceId) {
		return "lb://" + serviceId;
	}

	@Override
	public RouteDefinition convert2RouteDefinition(KylinRouteRaw rawRoute) throws URISyntaxException {
		RouteDefinition routeDefinition = new RouteDefinition();

		String uuid = String.valueOf(rawRoute.getId());
		routeDefinition.setId(uuid);

		PredicateDefinition predicateDefinition = new PredicateDefinition();
		routeDefinition.setUri(new URI(getStringURIByServiceId(getServiceId(rawRoute))));

		routeDefinition.setPredicates(Lists.newArrayList(predicateDefinition));

		KylinResourceGroupTypeEnum resourceGroupTypeEnum = KylinResourceGroupTypeEnum.valueOf(rawRoute.getType());
		switch (resourceGroupTypeEnum) {
			case QUERY:
				predicateDefinition.setName(KYLIN_ROUTE_PREDICATE);
				predicateDefinition.getArgs().put(PREDICATE_ARG_KEY_0, rawRoute.getProject());
				routeDefinition.setOrder(rawRoute.getOrder());
				break;
			case DEFAULT:
			case GLOBAL:
				predicateDefinition.setName("Path");
				predicateDefinition.getArgs().put(PREDICATE_ARG_KEY_0, "/**");
				routeDefinition.setOrder(Integer.MAX_VALUE);
				break;
			default:
				log.warn("Skip route, resource group type of route is {}", resourceGroupTypeEnum);
				routeDefinition.setBroken(true);
				break;
		}

		return routeDefinition;
	}

	@Override
	public KylinLoadBalancer convert2KylinLoadBalancer(KylinRouteRaw routeRaw) {
		KylinLoadBalancer kylinLoadBalancer =
				new KylinLoadBalancer(getServiceId(routeRaw), ping, new RoundRobinRule(), pingStrategy, globalConfig.getLastValidRawRouteTableMvcc().get());

		kylinLoadBalancer.addServers(routeRaw.getBackends());
		return kylinLoadBalancer;
	}

	@Override
	public KylinRouteTable convert(List<KylinRouteRaw> rawRouteTable) {
		Preconditions.checkArgument(CollectionUtils.isNotEmpty(rawRouteTable));

		KylinRouteTable routeTable = new KylinRouteTable();
		routeTable.setMvcc(globalConfig.getLastValidRawRouteTableMvcc().get());

		Map<String, BaseLoadBalancer> loadBalancerMap = Maps.newHashMap();

		boolean existGlobal = false;
		boolean existKylin = false;
		for (KylinRouteRaw rawRoute : rawRouteTable) {
			try {
				RouteDefinition routeDefinition = convert2RouteDefinition(rawRoute);
				if (routeDefinition.isBroken()) {
					continue;
				}

				KylinLoadBalancer loadBalancer;

				String serviceId = getServiceId(rawRoute);
				if (loadBalancerMap.containsKey(serviceId)) {
					if (!CollectionUtils.isEqualCollection(loadBalancerMap.get(serviceId).getAllServers(), rawRoute.getBackends())) {
						throw new RuntimeException("Same resource group, difference server list !");
					}

					loadBalancer = (KylinLoadBalancer) loadBalancerMap.get(serviceId);
				} else {
					loadBalancer = convert2KylinLoadBalancer(rawRoute);
					if (loadBalancer.isBroken()) {
						loadBalancer.shutdown();
						continue;
					}
				}

				loadBalancerMap.put(serviceId, loadBalancer);

				if (GLOBAL == KylinResourceGroupTypeEnum.valueOf(rawRoute.getType())) {
					existGlobal = true;
				}

				if (KYLIN_ROUTE_PREDICATE.equals(routeDefinition.getPredicates().get(0).getName())) {
					existKylin = true;
				}

				routeTable.addRoute(new KylinRouteTable.Route(routeDefinition, loadBalancer));
			} catch (Exception e) {
				log.error("Failed to convert rawRoute to KylinRouteTable, rawRoute: {}", rawRoute, e);
				routeTable.setBroken(true);
			}
		}

		if (!routeTable.isBroken() && existGlobal && !existKylin) {
			KylinRouteRaw guardRoute = new KylinRouteRaw();
			try {
				guardRoute.setId(Integer.MIN_VALUE);
				guardRoute.setOrder(Integer.MIN_VALUE);

				guardRoute.setCluster(rawRouteTable.get(0).getCluster());
				guardRoute.setBackends(Lists.newArrayList());

				guardRoute.setProject("GLOBAL-GUARD");
				guardRoute.setType(KylinResourceGroupTypeEnum.QUERY.name());

				RouteDefinition routeDefinition = convert2RouteDefinition(guardRoute);
				KylinLoadBalancer loadBalancer = convert2KylinLoadBalancer(guardRoute);

				routeTable.addRoute(new KylinRouteTable.Route(routeDefinition, loadBalancer));
			} catch (Exception e) {
				log.error("Failed to add guard route to KylinRouteTable, rawRoute: {}", guardRoute, e);
				routeTable.setBroken(true);
			}
		}

		return routeTable;
	}

}
