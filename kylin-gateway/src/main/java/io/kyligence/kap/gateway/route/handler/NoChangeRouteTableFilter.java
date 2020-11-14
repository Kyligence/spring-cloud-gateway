package io.kyligence.kap.gateway.route.handler;

import io.kyligence.kap.gateway.config.GlobalConfig;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(value = 0)
@Component
public class NoChangeRouteTableFilter implements RouteTableFilter {

	@Autowired
	private GlobalConfig globalConfig;

	@Override
	public boolean filter(List<KylinRouteRaw> rawRouteTable) {
		return CollectionUtils.isEqualCollection(rawRouteTable, globalConfig.getLastValidRawRouteTable());
	}

	@Override
	public List<String> getErrorMessage() {
		return null;
	}
}
