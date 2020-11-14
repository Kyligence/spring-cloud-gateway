package io.kyligence.kap.gateway.route.handler;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.constant.KylinResourceGroupTypeEnum;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static io.kyligence.kap.gateway.route.handler.RouteRegularRouteTableFilter.ROUTE_ILLEGAL_MESSAGE;

@Order(value = 102)
@Component
public class ResourceGroupTypeAggRouteTableFilter implements RouteTableFilter {

	@Override
	public boolean filter(List<KylinRouteRaw> rawRouteTable) {
		return rawRouteTable.stream().filter(kylinRouteRaw -> {
			KylinResourceGroupTypeEnum resourceGroupTypeEnum = KylinResourceGroupTypeEnum.valueOf(kylinRouteRaw.getType());
			return KylinResourceGroupTypeEnum.DEFAULT == resourceGroupTypeEnum || KylinResourceGroupTypeEnum.GLOBAL == resourceGroupTypeEnum;
		}).count() > 1;
	}

	@Override
	public List<String> getErrorMessage() {
		return Lists.newArrayList(String.format("Illegal: can not use resource group types %s together!",
				Arrays.toString(new KylinResourceGroupTypeEnum[]{KylinResourceGroupTypeEnum.DEFAULT, KylinResourceGroupTypeEnum.GLOBAL})),
				ROUTE_ILLEGAL_MESSAGE);
	}
}
