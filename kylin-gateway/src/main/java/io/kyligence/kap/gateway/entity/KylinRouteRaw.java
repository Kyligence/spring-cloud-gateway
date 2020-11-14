package io.kyligence.kap.gateway.entity;

import com.google.common.base.Preconditions;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.persistent.domain.Kylin3XRouteDO;
import io.kyligence.kap.gateway.persistent.domain.KylinRouteTableDO;
import io.kyligence.kap.gateway.persistent.domain.RouteDO;
import io.kyligence.kap.gateway.utils.JsonUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@NoArgsConstructor
public class KylinRouteRaw {

	private long id;

	private List<Server> backends;

	private String project;

	private String resourceGroup;

	private String type;

	private int order = 0;

	private String cluster;

	public static KylinRouteRaw convert(Kylin3XRouteDO kylin3XRouteDO) {
		if (null == kylin3XRouteDO) {
			log.error("Failed to convert KylinRouteDO to KylinRouteRaw, cause by kylinRouteDO is null!");
			return null;
		}

		KylinRouteRaw kylinRouteRaw = new KylinRouteRaw();

		kylinRouteRaw.id = kylin3XRouteDO.getId();

		kylinRouteRaw.cluster = kylin3XRouteDO.getCluster();

		kylinRouteRaw.project = kylin3XRouteDO.getProject();
		kylinRouteRaw.type = kylin3XRouteDO.getType();

		kylinRouteRaw.resourceGroup = kylin3XRouteDO.getResourceGroup();

		try {
			Preconditions.checkNotNull(kylin3XRouteDO.getBackends(), "Route backends is null !");

			List<String> instances = JsonUtil.toObject(kylin3XRouteDO.getBackends(), List.class);

			Preconditions.checkNotNull(instances);

			kylinRouteRaw.backends = instances.stream().map(Server::new).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Failed to read backends.", e);
		}

		return kylinRouteRaw;
	}

	public static KylinRouteRaw convert(KylinRouteTableDO.KylinRouteDO kylinRouteDO) {
		if (null == kylinRouteDO) {
			log.error("Failed to convert KylinRouteDO to KylinRouteRaw, cause by kylinRouteDO is null!");
			return null;
		}

		KylinRouteRaw kylinRouteRaw = new KylinRouteRaw();

		kylinRouteRaw.setId(kylinRouteDO.getId());
		kylinRouteRaw.setOrder(0);

		kylinRouteRaw.setCluster(kylinRouteDO.getClusterId());
		kylinRouteRaw.setBackends(kylinRouteDO.getBackends());

		kylinRouteRaw.setProject(kylinRouteDO.getProject());
		kylinRouteRaw.setType(kylinRouteDO.getType());

		kylinRouteRaw.setResourceGroup(kylinRouteDO.getResourceGroup());

		return kylinRouteRaw;
	}

	public static KylinRouteRaw convert(RouteDO routeDO) {
		if (null == routeDO) {
			return null;
		}

		KylinRouteRaw kylinRouteRaw = new KylinRouteRaw();

		kylinRouteRaw.setId(routeDO.getId());
		kylinRouteRaw.setOrder(routeDO.getOrder());

		kylinRouteRaw.setCluster(routeDO.getCluster());
		kylinRouteRaw.setBackends(routeDO.getBackends().stream().map(Server::new).collect(Collectors.toList()));

		kylinRouteRaw.setProject(routeDO.getProject());
		kylinRouteRaw.setType(routeDO.getType());

		kylinRouteRaw.setResourceGroup(routeDO.getResourceGroup());

		return kylinRouteRaw;
	}

	@Override
	public String toString() {
		return JsonUtil.toJson(this);
	}

}
