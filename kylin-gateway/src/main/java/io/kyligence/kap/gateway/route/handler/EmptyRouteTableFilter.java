package io.kyligence.kap.gateway.route.handler;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;


@Order(value = Integer.MIN_VALUE)
@Component
public class EmptyRouteTableFilter implements RouteTableFilter {

	@Override
	public boolean filter(List<KylinRouteRaw> rawRouteTable) {
		return CollectionUtils.isEmpty(rawRouteTable);
	}

	@Override
	public List<String> getErrorMessage() {
		return Lists.newArrayList("Failed to refresh route table, cause by new route table is empty!");
	}
}
