package io.kyligence.kap.gateway.persistent.domain;

import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KylinRouteTableDO {

	private long id;

	private String cluster;

	private String service;

	private List<KylinRouteDO> routeTable;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class KylinRouteDO {

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
}