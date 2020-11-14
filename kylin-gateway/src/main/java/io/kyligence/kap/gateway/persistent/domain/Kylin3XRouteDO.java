package io.kyligence.kap.gateway.persistent.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author zhiyu.zeng
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Kylin3XRouteDO {

	private long id;

	private String backends;

	private String project;

	private String resourceGroup;

	private String type;

	private String cluster;

	@Override
	public String toString() {
		return "KylinRouteDO{" + "id=" + id + ", backends='" + backends + '\''
				+ ", project='" + project + '\'' + ", resourceGroup='" + resourceGroup
				+ '\'' + ", type='" + type + '\'' + ", cluster='" + cluster + '\'' + '}';
	}

}
