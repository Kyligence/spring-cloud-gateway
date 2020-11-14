package io.kyligence.kap.gateway.route;

import com.google.common.collect.Lists;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IPingStrategy;
import com.netflix.loadbalancer.RoundRobinRule;
import io.kyligence.kap.gateway.constant.Kylin3xResourceGroupTypeEnum;
import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.event.KylinRefreshRoutesEvent;
import io.kyligence.kap.gateway.filter.KylinLoadBalancer;
import io.kyligence.kap.gateway.health.KylinPing;
import io.kyligence.kap.gateway.route.reader.IRouteTableReader;
import io.kyligence.kap.gateway.utils.AsyncQueryUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static io.kyligence.kap.gateway.constant.KylinRouteConstant.DEFAULT_RESOURCE_GROUP;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.KYLIN_ROUTE_PREDICATE;
import static io.kyligence.kap.gateway.constant.KylinRouteConstant.PREDICATE_ARG_KEY_0;

@ConditionalOnProperty(name = "kylin.gateway.ke.version", havingValue = KylinGatewayVersion.KYLIN_3X)
@Component
@EnableScheduling
public class Kylin3xRefreshRouteTableScheduler implements ApplicationEventPublisherAware {

	private static final Logger logger = LoggerFactory.getLogger(Kylin3xRefreshRouteTableScheduler.class);

	protected ApplicationEventPublisher publisher;

	private AbstractGatewayControllerEndpoint gatewayControllerEndpoint;

	private LoadBalancerClientFilter loadBalancerClientFilter;

	private IRouteTableReader routeTableReader;

	@Autowired
	private KylinPing ping;

	@Autowired
	private IPingStrategy pingStrategy;

	private AtomicLong mvcc = new AtomicLong(0);

	private List<KylinRouteRaw> oldRouteRawList = Lists.newArrayList();

	public Kylin3xRefreshRouteTableScheduler(IRouteTableReader routeTableReader,
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

	private String project2ServiceId(String project) {
		if (StringUtils.isBlank(project)) {
			return project;
		}

		return UUID.nameUUIDFromBytes(project.getBytes()).toString().substring(0, 9) + project.replace('_', '-');
	}

	private String getStringURI(String serviceId) {
		return "lb://" + serviceId;
	}

	private String getServiceId(KylinRouteRaw routeRaw, boolean skipAsync) {
		String serviceId = project2ServiceId(routeRaw.getProject());
		switch (Kylin3xResourceGroupTypeEnum.valueOf(routeRaw.getType())) {
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

		switch (Kylin3xResourceGroupTypeEnum.valueOf(routeRaw.getType())) {
			case ASYNC:
			case CUBE:
				predicateDefinition.setName(KYLIN_ROUTE_PREDICATE);
				predicateDefinition.getArgs().put(PREDICATE_ARG_KEY_0, routeRaw.getProject());
				routeDefinition.setOrder(routeRaw.getOrder());
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

	private KylinLoadBalancer convert2Kylin3XLoadBalancer(KylinRouteRaw routeRaw) {
		KylinLoadBalancer kylin3XLoadBalancer =
				new KylinLoadBalancer(getServiceId(routeRaw, false), ping, new RoundRobinRule(), pingStrategy, this.mvcc.get());

		kylin3XLoadBalancer.addServers(routeRaw.getBackends());
		return kylin3XLoadBalancer;
	}

	private boolean isRawRouteTableIllegal(List<KylinRouteRaw> routeRawList) {
		boolean checkResult = false;

		if (routeRawList.size() > routeRawList.stream().map(KylinRouteRaw::getId).distinct().count()) {
			logger.error("Route table contain same id!");
			return true;
		}

		List<KylinRouteRaw> errorList = routeRawList.stream().filter(kylinRouteRaw -> {
			try {
				Kylin3xResourceGroupTypeEnum.valueOf(kylinRouteRaw.getType());
			} catch (IllegalArgumentException e) {
				return true;
			}

			if (StringUtils.isBlank(kylinRouteRaw.getProject())
					&& Kylin3xResourceGroupTypeEnum.valueOf(kylinRouteRaw.getType()) != Kylin3xResourceGroupTypeEnum.GLOBAL) {
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

	@Scheduled(cron = "${kylin.gateway.route.refresh-cron}")
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
					KylinLoadBalancer loadBalancer = convert2Kylin3XLoadBalancer(routeRaw);

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

			publisher.publishEvent(new KylinRefreshRoutesEvent(this));
			this.loadBalancerClientFilter.updateResourceGroups(loadBalancerList, this.mvcc.get());

			this.mvcc.incrementAndGet();
			this.oldRouteRawList = routeRawList;
			logger.info("Update route table is success ...");
		} catch (Exception e) {
			logger.error("Failed to get route table from {}!", routeTableReader.getClass(), e);
		}

	}

}
