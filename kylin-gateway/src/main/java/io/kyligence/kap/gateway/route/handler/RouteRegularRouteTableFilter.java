package io.kyligence.kap.gateway.route.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.constant.KylinResourceGroupTypeEnum;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Order(value = 100)
@Component
public class RouteRegularRouteTableFilter implements RouteTableFilter {

	public static final String ROUTE_ILLEGAL_MESSAGE = "Failed to refresh route table, cause by new route table illegal!";

	public static final String ERROR_RESOURCE_GROUP = "ERROR_RESOURCE_GROUP";

	public static final String ERROR_RESOURCE_GROUP_TYPE = "ERROR_RESOURCE_GROUP_TYPE";

	public static final String ERROR_PROJECT = "ERROR_PROJECT";

	public static final String ERROR_BACKENDS = "ERROR_BACKENDS";

	public static final String ERROR_SAME_RESOURCE_GROUP_DIFF_BACKENDS = "ERROR_SAME_RESOURCE_GROUP_DIFF_BACKENDS";

	public static final String ERROR_CODE = "ERROR_CODE";

	private List<KylinRouteRaw> errorList;

	private List<String> errorMessages;

	@Override
	public boolean filter(List<KylinRouteRaw> rawRouteTable) {
		errorMessages = Lists.newArrayList();

		Map<String, List<Server>> serverListMap = Maps.newHashMap();

		errorList = rawRouteTable.stream().filter(kylinRouteRaw -> {
			KylinResourceGroupTypeEnum resourceGroupTypeEnum;
			try {
				resourceGroupTypeEnum = KylinResourceGroupTypeEnum.valueOf(kylinRouteRaw.getType());
			} catch (IllegalArgumentException e) {
				errorMessages.add(ERROR_RESOURCE_GROUP_TYPE);
				return true;
			}

			if (StringUtils.isBlank(kylinRouteRaw.getResourceGroup())) {
				errorMessages.add(ERROR_RESOURCE_GROUP);
				return true;
			}

			if (StringUtils.isBlank(kylinRouteRaw.getProject())
					&& KylinResourceGroupTypeEnum.DEFAULT != resourceGroupTypeEnum
					&& KylinResourceGroupTypeEnum.GLOBAL != resourceGroupTypeEnum) {
				errorMessages.add(ERROR_PROJECT);
				return true;
			}

			if (Objects.isNull(kylinRouteRaw.getBackends())) {
				errorMessages.add(ERROR_BACKENDS);
				return true;
			}

			if (serverListMap.containsKey(kylinRouteRaw.getResourceGroup())) {
				if (!CollectionUtils.isEqualCollection(serverListMap.get(kylinRouteRaw.getResourceGroup()), kylinRouteRaw.getBackends())) {
					errorMessages.add(ERROR_SAME_RESOURCE_GROUP_DIFF_BACKENDS);
					return true;
				}
			} else {
				serverListMap.put(kylinRouteRaw.getResourceGroup(), kylinRouteRaw.getBackends());
			}

			return false;
		}).collect(Collectors.toList());

		return CollectionUtils.isNotEmpty(errorList);
	}

	@Override
	public List<String> getErrorMessage() {
		List<String> resultMessages = Lists.newArrayList();
		if (CollectionUtils.isEmpty(errorList)) {
			return resultMessages;
		}

		if (CollectionUtils.isEmpty(errorMessages)) {
			errorMessages = Lists.newArrayList();
		}

		if (errorMessages.size() < errorList.size()) {
			int delta = errorList.size() - errorMessages.size();
			for (int i = 0; i < delta; i++) {
				errorMessages.add(ERROR_CODE);
			}
		}

		int i = 0;
		for (KylinRouteRaw kylinRouteRaw : errorList) {
			resultMessages.add(String.format("Illegal Route(%s): %s", errorMessages.get(i++), kylinRouteRaw));
		}

		resultMessages.add(ROUTE_ILLEGAL_MESSAGE);
		return resultMessages;
	}
}
