package io.kyligence.kap.gateway.persistent.domain;

import lombok.AllArgsConstructor;
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

}