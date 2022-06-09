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
public class MdxRouteDO {

	private String type;

	private String strategy;

	private String host;

	private List<Server> backends;

	@Override
	public String toString() {
		return JsonUtil.toJson(this);
	}

}