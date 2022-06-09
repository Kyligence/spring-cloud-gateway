package io.kyligence.kap.gateway.persistent.domain;

import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KylinRouteDO {

	private long id;

	private String clusterId;

	private List<Server> backends;

	private String project;

	private String type;

	private String resourceGroup;

	@Override
	public String toString() {
		return JsonUtil.toJson(this);
	}
}