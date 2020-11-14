package io.kyligence.kap.gateway.route.handler;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(value = Integer.MIN_VALUE + 1)
@Component
public class NullExistRouteTableFilter implements RouteTableFilter {
	@Override
	public boolean filter(List<KylinRouteRaw> rawRouteTable) {
		if (null == rawRouteTable) {
			return true;
		}

		for (Object obj : rawRouteTable) {
			if (null == obj) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<String> getErrorMessage() {
		return Lists.newArrayList("Failed to refresh route table, cause by new route table is empty!");
	}
}
