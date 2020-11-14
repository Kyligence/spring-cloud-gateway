package io.kyligence.kap.gateway.route.transformer;

import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.entity.KylinRouteTable;
import io.kyligence.kap.gateway.filter.KylinLoadBalancer;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;

public interface RouteTableTransformer {

	RouteDefinition convert2RouteDefinition(KylinRouteRaw routeRaw) throws Exception;

	KylinLoadBalancer convert2KylinLoadBalancer(KylinRouteRaw routeRaw) throws Exception;

	KylinRouteTable convert(List<KylinRouteRaw> rawRouteTable);

}
