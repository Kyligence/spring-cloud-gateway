package io.kyligence.kap.gateway.route.handler;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.kyligence.kap.gateway.route.handler.RouteRegularRouteTableFilter.ROUTE_ILLEGAL_MESSAGE;

@Order(value = 101)
@Component
public class UniqueIDAggRouteTableFilter implements RouteTableFilter {
	@Override
	public boolean filter(List<KylinRouteRaw> rawRouteTable) {
		return rawRouteTable.size() > rawRouteTable.stream().map(KylinRouteRaw::getId).distinct().count();
	}

	@Override
	public List<String> getErrorMessage() {
		return Lists.newArrayList("Illegal: route table contain same id!", ROUTE_ILLEGAL_MESSAGE);
	}
}
