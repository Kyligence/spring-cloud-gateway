package io.kyligence.kap.gateway.route;

import com.google.common.collect.Lists;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.RoundRobinRule;
import io.kyligence.kap.gateway.constant.KylinResourceGroupTypeEnum;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.event.Kylin3XRefreshRoutesEvent;
import io.kyligence.kap.gateway.filter.Kylin3XLoadBalancer;
import io.kyligence.kap.gateway.health.KylinPing;
import io.kyligence.kap.gateway.utils.AsyncQueryUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.actuate.AbstractGatewayControllerEndpoint;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.kyligence.kap.gateway.constant.KylinRouteConstant.DEFAULT_RESOURCE_GROUP;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.KYLIN_ROUTE_PREDICATE;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.PREDICATE_ARG_KEY_0;

@Component
@EnableScheduling
public class RefreshRouteTableScheduler implements ApplicationEventPublisherAware {

	private static final Logger logger = LoggerFactory.getLogger(RefreshRouteTableScheduler.class);

	protected ApplicationEventPublisher publisher;

	private AbstractGatewayControllerEndpoint gatewayControllerEndpoint;

	private LoadBalancerClientFilter loadBalancerClientFilter;

	private IRouteTableReader routeTableReader;

	@Autowired
	private KylinPing ping;

	@Autowired
	private IPingStrategy pingStrategy;

	private List<KylinRouteRaw> oldRouteRawList = Lists.newArrayList();

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

	private String getStringURI(String project) {
		return "lb://" + project.replace('_', '-');
	}

	private String addKylinHeader(String project) {
		return "kylin-" + project;
	}

	private String getServiceId(KylinRouteRaw routeRaw, boolean skipAsync) {
		String serviceId = addKylinHeader(routeRaw.getProject());
		switch (KylinResourceGroupTypeEnum.valueOf(routeRaw.getType())) {
			case GLOBAL:
				serviceId = DEFAULT_RESOURCE_GROUP;
				break;
			case ASYNC:
				if (!skipAsync) {
					serviceId = AsyncQueryUtil.buildAsyncQueryServiceId(serviceId);
				}
				break;
			default:
				break;
		}

		return serviceId;
	}

	private RouteDefinition convert2RouteDefinition(KylinRouteRaw routeRaw)
			throws URISyntaxException {
		RouteDefinition routeDefinition = new RouteDefinition();

		String uuid = String.valueOf(routeRaw.getId());
		routeDefinition.setId(uuid);

		PredicateDefinition predicateDefinition = new PredicateDefinition();
		routeDefinition.setPredicates(Lists.newArrayList(predicateDefinition));

		switch (KylinResourceGroupTypeEnum.valueOf(routeRaw.getType())) {
			case ASYNC:
			case CUBE:
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

		routeDefinition.setUri(new URI(getStringURI(getServiceId(routeRaw, true))));
		return routeDefinition;
	}

	private Kylin3XLoadBalancer convert2Kylin3XLoadBalancer(KylinRouteRaw routeRaw) {
		Kylin3XLoadBalancer kylin3XLoadBalancer =
				new Kylin3XLoadBalancer(getServiceId(routeRaw, false), ping, new RoundRobinRule(), pingStrategy);

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
			} catch (URISyntaxException e) {
				return true;
			}

			if (CollectionUtils.isEmpty(kylinRouteRaw.getBackends())) {
				return true;
			}

			return false;
		}).collect(Collectors.toList());

		if (CollectionUtils.isNotEmpty(errorList)) {
			checkResult = true;
			errorList.forEach(kylinRouteRaw -> logger.error("Error Route: {}", kylinRouteRaw));
		}

		return checkResult;
	}

	private boolean isRouteTableNotChange(List<KylinRouteRaw> newRouteRawList) {
		return CollectionUtils.isEqualCollection(newRouteRawList, oldRouteRawList);
	}

	private boolean isNullExist(Collection objectList) {
		if (null == objectList) {
			return true;
		}

		for (Object obj : objectList) {
			if (null == obj) {
				return true;
			}
		}
		return false;
	}

	@Scheduled(cron = "${kylin.gateway.route-table.refresh-cron}")
	public synchronized void run() {
		try {
			List<KylinRouteRaw> routeRawList = routeTableReader.list();
			if (CollectionUtils.isEmpty(routeRawList)) {
				// do not permit to clear route table
				logger.error("Failed to refresh route table, cause by new route table is empty!");
				return;
			}

			if (isNullExist(routeRawList)) {
				logger.error("Failed to refresh route table, cause by new route table null exist!");
				return;
			}

			if (isRouteTableNotChange(routeRawList)) {
				return;
			}

			if (isRawRouteTableIllegal(routeRawList)) {
				logger.error("Failed to refresh route table, cause by new route table illegal!");
				return;
			}

			logger.info("Start to update route table ...");

			List<RouteDefinition> routeDefinitionList = Lists.newArrayList();
			List<BaseLoadBalancer> loadBalancerList = Lists.newArrayList();
			for (KylinRouteRaw routeRaw : routeRawList) {
				try {
					RouteDefinition routeDefinition = convert2RouteDefinition(routeRaw);
					Kylin3XLoadBalancer loadBalancer = convert2Kylin3XLoadBalancer(routeRaw);

					routeDefinitionList.add(routeDefinition);
					loadBalancerList.add(loadBalancer);
				} catch (Exception e) {
					logger.error("Failed to convert KylinRouteRaw, {}", routeRaw, e);
					return;
				}
			}

			this.loadBalancerClientFilter.addResourceGroups(loadBalancerList);

			this.oldRouteRawList.forEach(kylinRouteRaw ->
					gatewayControllerEndpoint.delete(String.valueOf(kylinRouteRaw.getId())).subscribe());

			routeDefinitionList.forEach(routeDefinition ->
					gatewayControllerEndpoint.save(routeDefinition.getId(), routeDefinition).subscribe());

			publisher.publishEvent(new Kylin3XRefreshRoutesEvent(this));
			this.loadBalancerClientFilter.updateResourceGroups(loadBalancerList);

			this.oldRouteRawList = routeRawList;
			logger.info("Update route table is success ...");
		} catch (Exception e) {
			logger.error("Failed to get route table from {}!", routeTableReader.getClass(), e);
		}

	}

}
