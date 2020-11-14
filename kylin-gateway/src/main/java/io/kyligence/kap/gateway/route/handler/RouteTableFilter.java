package io.kyligence.kap.gateway.route.handler;

import io.kyligence.kap.gateway.entity.KylinRouteRaw;

import java.util.List;

public interface RouteTableFilter {

	boolean filter(List<KylinRouteRaw> rawRouteTable);

	List<String> getErrorMessage();
}
