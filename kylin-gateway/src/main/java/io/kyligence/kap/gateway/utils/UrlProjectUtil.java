package io.kyligence.kap.gateway.utils;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlProjectUtil {
	public static final String URL_PROJECTS_PREFIX = "/kylin/api/projects";

	public static final String URL_MODELS_PREFIX = "/kylin/api/models";

	private static final Pattern[] URL_PROJECT_PATTERNS = new Pattern[]{
			Pattern.compile("^/kylin/api/projects/([^/]+)/(backup|default_database" +
					"|query_accelerate_threshold|storage" +
					"|storage_quota|shard_num_config" +
					"|garbage_cleanup_config|job_notification_config" +
					"|push_down_config|push_down_project_config|computed_column_config" +
					"|segment_config|project_general_info" +
					"|project_config|source_type" +
					"|yarn_queue|project_kerberos_info" +
					"|owner|config)$"),
			Pattern.compile("^/kylin/api/projects/([^/]+)$"),
			Pattern.compile("^/kylin/api/models/([^/]+)/[^/]+/partition_desc$")
	};

	private UrlProjectUtil() {

	}

	public static String extractProjectFromUrlPath(ServerWebExchange exchange) {
		String urlPath = exchange.getRequest().getPath().toString();
		if (StringUtils.isBlank(urlPath)
				|| !(urlPath.startsWith(URL_PROJECTS_PREFIX) || urlPath.startsWith(URL_MODELS_PREFIX))) {
			return null;
		}

		for (Pattern pattern : URL_PROJECT_PATTERNS) {
			Matcher matcher = pattern.matcher(urlPath);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return null;
	}

}
