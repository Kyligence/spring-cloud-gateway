package io.kyligence.kap.gateway.loadbalancer;

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Component
@Slf4j
@ConditionalOnProperty(name = "server.type", havingValue = KylinGatewayVersion.KYLIN_4X)
public class KylinPing implements IPing {

	private static final String HEALTH_URL_FORMAT = "http://%s%s";

	@Autowired
	private RestTemplate restTemplate;

	@Value("${kylin.gateway.health.check-url:/kylin/api/health}")
	private String healthUrl;

	@Override
	public boolean isAlive(Server server) {
		return ErrorLevel.NORMAL == checkServer(server);
	}

	public ErrorLevel checkServer(Server server) {
		if (Objects.isNull(server)) {
			return ErrorLevel.FATAL;
		}

		String healthCheckUrl = String.format(HEALTH_URL_FORMAT, server.getId(), healthUrl);

		try {
			ResponseEntity<String> responseEntity = restTemplate.getForEntity(healthCheckUrl, String.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return ErrorLevel.NORMAL;
			}
		} catch (Exception e) {
			log.warn("health check failed, server: {}", server);
			log.debug("health check failed!", e);

			if (e instanceof ResourceAccessException) {
				return ErrorLevel.FATAL;
			}

			return ErrorLevel.ERROR;
		}

		return ErrorLevel.WARN;
	}

	public enum ErrorLevel {
		NORMAL,
		WARN,
		ERROR,
		FATAL,
	}

}
