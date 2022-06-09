package io.kyligence.kap.gateway.route.transformer;

import com.netflix.loadbalancer.BaseLoadBalancer;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.entity.KylinRouteTable;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;

public interface RouteTableTransformer {

	RouteDefinition convert2RouteDefinition(KylinRouteRaw routeRaw) throws Exception;

	BaseLoadBalancer convert2KylinLoadBalancer(KylinRouteRaw routeRaw) throws Exception;

	KylinRouteTable convert(List<KylinRouteRaw> rawRouteTable);

}
