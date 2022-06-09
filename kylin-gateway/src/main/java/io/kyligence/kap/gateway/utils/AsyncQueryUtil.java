package io.kyligence.kap.gateway.utils;

import com.google.common.base.Preconditions;

public class AsyncQueryUtil {

	public static final String ASYNC_QUERY_SUFFIX_TAG = "ASYNC";

	private AsyncQueryUtil() {
	}

	public static String buildAsyncQueryServiceId(String serviceId) {
		Preconditions.checkNotNull(serviceId);

		return serviceId + "-" + ASYNC_QUERY_SUFFIX_TAG;
	}

}
