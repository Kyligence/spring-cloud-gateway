package io.kyligence.kap.gateway.route;

import com.google.common.collect.Lists;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.DummyPing;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.PingUrl;
import com.netflix.loadbalancer.RoundRobinRule;
import io.kyligence.kap.gateway.constant.KylinResourceGroupTypeEnum;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.filter.Kylin3XLoadBalancer;
import io.kyligence.kap.gateway.health.ConcurrentPingStrategy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.actuate.AbstractGatewayControllerEndpoint;
import org.springframework.cloud.gateway.actuate.GatewayControllerEndpoint;
import org.springframework.cloud.gateway.actuate.GatewayLegacyControllerEndpoint;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.kyligence.kap.gateway.constant.KylinRouteConstant.KYLIN_ROUTE_PREDICATE;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.PREDICATE_ARG_KEY_0;

@Component
@EnableScheduling
public class RefreshRouteTableScheduler implements ApplicationEventPublisherAware {

	private static final Logger logger = LoggerFactory
			.getLogger(RefreshRouteTableScheduler.class);

	protected ApplicationEventPublisher publisher;

	private AbstractGatewayControllerEndpoint gatewayControllerEndpoint;

	private LoadBalancerClientFilter loadBalancerClientFilter;

	private IRouteTableReader routeTableReader;

	@Autowired
	private PingUrl ping;

	@Autowired
	private IPingStrategy pingStrategy;

	public RefreshRouteTableScheduler(IRouteTableReader routeTableReader,
			AbstractGatewayControllerEndpoint gatewayControllerEndpoint,
			LoadBalancerClientFilter loadBalancerClientFilter) {
		this.routeTableReader = routeTableReader;
		this.gatewayControllerEndpoint = gatewayControllerEndpoint;
		this.loadBalancerClientFilter = loadBalancerClientFilter;
	}

	@PostConstruct
	private void init() {
		run();
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	private String getStringURI(String resourceGroup) {
		return "lb://" + resourceGroup;
	}

	private RouteDefinition convert2RouteDefinition(KylinRouteRaw routeRaw)
			throws URISyntaxException {
		RouteDefinition routeDefinition = new RouteDefinition();

		String uuid = routeRaw.getCluster() + "-" + routeRaw.getId();
		routeDefinition.setId(uuid);

		PredicateDefinition predicateDefinition = new PredicateDefinition();
		routeDefinition.setPredicates(Lists.newArrayList(predicateDefinition));

		switch (KylinResourceGroupTypeEnum.valueOf(routeRaw.getType())) {
		case CUBE:
		case ASYNC:
			predicateDefinition.setName(KYLIN_ROUTE_PREDICATE);
			predicateDefinition.getArgs().put(PREDICATE_ARG_KEY_0, routeRaw.getProject());
			routeDefinition.setOrder(0);
			break;
		case GLOBAL:
			predicateDefinition.setName("Path");
			predicateDefinition.getArgs().put(PREDICATE_ARG_KEY_0, "/**");
			routeDefinition.setOrder(Integer.MAX_VALUE);
			break;
		default:
			routeDefinition.setOrder(Integer.MAX_VALUE - 1);
			logger.warn("Route Table must have type!");
		}

		routeDefinition.setUri(new URI(getStringURI(routeRaw.getResourceGroup())));

		return routeDefinition;
	}

	private Kylin3XLoadBalancer convert2Kylin3XLoadBalancer(KylinRouteRaw routeRaw) {
		Kylin3XLoadBalancer kylin3XLoadBalancer = new Kylin3XLoadBalancer(
				routeRaw.getResourceGroup(), ping, new RoundRobinRule(), pingStrategy);

		kylin3XLoadBalancer.addServers(routeRaw.getBackends());
		return kylin3XLoadBalancer;
	}

	private boolean isRawRouteTableIllegal(List<KylinRouteRaw> routeRawList) {
		boolean checkResult = false;

		List<KylinRouteRaw> errorList = routeRawList.stream().filter(kylinRouteRaw -> {
			if (kylinRouteRaw.getId() < 0) {
				return true;
			}

			if (StringUtils.isBlank(kylinRouteRaw.getProject())
					|| StringUtils.isBlank(kylinRouteRaw.getResourceGroup())
					|| StringUtils.isBlank(kylinRouteRaw.getStringBackends())
					|| StringUtils.isBlank(kylinRouteRaw.getType())) {
				return true;
			}

			try {
				URI uri = new URI(getStringURI(kylinRouteRaw.getResourceGroup()));
				if (Objects.isNull(uri.getHost()) || Objects.isNull(uri.getAuthority())
						|| Objects.isNull(uri.getScheme())) {
					return true;
				}
			}
			catch (URISyntaxException e) {
				return true;
			}

			if (CollectionUtils.isEmpty(kylinRouteRaw.getBackends())) {
				return true;
			}

			return false;
		}).collect(Collectors.toList());

		if (CollectionUtils.isNotEmpty(errorList)) {
			checkResult = true;
			errorList.stream().forEach(
					kylinRouteRaw -> logger.error("Error Route: {}", kylinRouteRaw));
		}

		return checkResult;
	}

	@Scheduled(cron = "${kylin.gateway.route-table.refresh-cron}")
	public void run() {
		try {
			List<KylinRouteRaw> routeRawList = this.routeTableReader.list();
			if (CollectionUtils.isEmpty(routeRawList)) {
				return;
			}

			if (isRawRouteTableIllegal(routeRawList)) {
				return;
			}

			List<RouteDefinition> routeDefinitionList = Lists.newArrayList();
			List<BaseLoadBalancer> loadBalancerList = Lists.newArrayList();
			for (KylinRouteRaw routeRaw : routeRawList) {
				try {
					RouteDefinition routeDefinition = convert2RouteDefinition(routeRaw);
					Kylin3XLoadBalancer loadBalancer = convert2Kylin3XLoadBalancer(
							routeRaw);

					routeDefinitionList.add(routeDefinition);
					loadBalancerList.add(loadBalancer);
				}
				catch (Exception e) {
					logger.error("Failed to convert KylinRouteRaw, {}", routeRaw, e);
				}
			}

			this.loadBalancerClientFilter.addResourceGroups(loadBalancerList);

			if (gatewayControllerEndpoint instanceof GatewayControllerEndpoint) {
				((GatewayControllerEndpoint) gatewayControllerEndpoint).routes()
						.subscribe(tRoute -> {
							gatewayControllerEndpoint
									.delete((String) tRoute.get("route_id")).subscribe();
						});
			}
			else if (gatewayControllerEndpoint instanceof GatewayLegacyControllerEndpoint) {
				((GatewayLegacyControllerEndpoint) gatewayControllerEndpoint).routes()
						.subscribe(tRouteList -> {
							tRouteList.forEach(tRoute -> {
								gatewayControllerEndpoint
										.delete((String) tRoute.get("route_id"))
										.subscribe();
							});
						});
			}

			routeDefinitionList.forEach(routeDefinition -> {
				gatewayControllerEndpoint.save(routeDefinition.getId(), routeDefinition)
						.subscribe();
			});

			publisher.publishEvent(new RefreshRoutesEvent(this));
			this.loadBalancerClientFilter.updateResourceGroups(loadBalancerList);
		}
		catch (Exception e) {
			logger.error("Failed to get route table from {}!",
					routeTableReader.getClass(), e);
		}

	}

}
