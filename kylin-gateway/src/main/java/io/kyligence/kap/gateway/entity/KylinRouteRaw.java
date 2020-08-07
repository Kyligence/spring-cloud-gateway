package io.kyligence.kap.gateway.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.persistent.domain.KylinRouteDO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
public class KylinRouteRaw {

	private long id;

	private String stringBackends;

	private List<Server> backends;

	private String project;

	private String resourceGroup;

	private String type;

	private String cluster;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public KylinRouteRaw(KylinRouteDO kylinRouteDO) {
		this.id = kylinRouteDO.getId();
		this.project = kylinRouteDO.getProject();
		this.resourceGroup = kylinRouteDO.getResourceGroup();
		this.type = kylinRouteDO.getType();
		this.cluster = kylinRouteDO.getCluster();
		this.stringBackends = kylinRouteDO.getBackends();
		try {
			List<String> instances = OBJECT_MAPPER.readValue(kylinRouteDO.getBackends(),
					List.class);
			backends = instances.stream().map(Server::new).collect(Collectors.toList());
		}
		catch (JsonProcessingException e) {
			log.error("Failed to read backends.", e);
		}
	}

	@Override
	public String toString() {
		return "KylinRouteRaw{" + "id=" + id + ", backends='" + stringBackends + '\''
				+ ", project='" + project + '\'' + ", resourceGroup='" + resourceGroup
				+ '\'' + ", type='" + type + '\'' + ", cluster='" + cluster + '\'' + '}';
	}

}
