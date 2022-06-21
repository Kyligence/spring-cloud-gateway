package io.kyligence.kap.gateway.loadbalancer;

import com.alibaba.fastjson.JSONObject;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import io.kyligence.kap.gateway.utils.Encryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Slf4j
@ConditionalOnProperty(name = "server.type", havingValue = KylinGatewayVersion.MDX)
public class MdxPing implements IPing {

	public final static String BASIC_AUTH_PREFIX = "Basic ";
	private static final String HEALTH_URL_FORMAT = "http://%s%s";
	private static final Double MAX_LOAD = 100.0d;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${mdx.check-url:/api/system/health}")
	private String healthUrl;

	@Value("${mdx.check-project:}")
	private String projectName;

	@Value("${mdx.load-url:/api/system/load}")
	private String loadUrl;

	@Value("${mdx.generate-schema-url:/mdx/xmla/}")
	private String generateSchemaUrl;

	@Value("${mdx.sync-username:}")
	private String syncUsername;

	@Value("${mdx.sync-password:}")
	private String syncPassword;

	public static HttpEntity<String> getCheckDatasetHttpEntity(String basicAuth) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add("Authorization", basicAuth);
		requestHeaders.add("Content-Type", "text/xml");
		requestHeaders.add("SOAPAction", "urn:schemas-microsoft-com:xml-analysis:Discover");
		requestHeaders.add("User-Agent", "MSOLAP 15.0 Client");
		String requestBody = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"\t<soap:Header>\n" +
				"\t\t<Session xmlns=\"urn:schemas-microsoft-com:xml-analysis\" SessionId=\"no_session\" />\n" +
				"\t</soap:Header>\n" +
				"\t<soap:Body>\n" +
				"\t\t<Discover xmlns=\"urn:schemas-microsoft-com:xml-analysis\">\n" +
				"\t\t\t<RequestType>DISCOVER_PROPERTIES</RequestType>\n" +
				"\t\t\t<Restrictions>\n" +
				"\t\t\t\t<RestrictionList>\n" +
				"\t\t\t\t\t<PropertyName>Catalog</PropertyName>\n" +
				"\t\t\t\t</RestrictionList>\n" +
				"\t\t\t</Restrictions>\n" +
				"\t\t\t<Properties>\n" +
				"\t\t\t\t<PropertyList>\n" +
				"\t\t\t\t\t<DbpropMsmdOptimizeResponse>9</DbpropMsmdOptimizeResponse>\n" +
				"\t\t\t\t\t<DbpropMsmdActivityID>7592EAA7-C793-416F-96BF-0749BFB18C25</DbpropMsmdActivityID>\n" +
				"\t\t\t\t\t<DbpropMsmdRequestID>58A9982A-9561-4E46-904B-8F0AA4F6849D</DbpropMsmdRequestID>\n" +
				"\t\t\t\t\t<LocaleIdentifier>2052</LocaleIdentifier>\n" +
				"\t\t\t\t</PropertyList>\n" +
				"\t\t\t</Properties>\n" +
				"\t\t</Discover>\n" +
				"\t</soap:Body>\n" +
				"</soap:Envelope>";
		return new HttpEntity<>(requestBody, requestHeaders);
	}

	public static String buildBasicAuth(String username, String password) {
		String encodedAuth = Base64.encodeBase64String((username + ":" + password).getBytes(StandardCharsets.UTF_8));
		return BASIC_AUTH_PREFIX + encodedAuth;
	}

	@Override
	public boolean isAlive(Server server) {
		return ErrorLevel.NORMAL == checkServer(server);
	}

	public ErrorLevel checkServer(Server server) {
		if (Objects.isNull(server)) {
			return ErrorLevel.FATAL;
		}

		String healthCheckUrl = String.format(HEALTH_URL_FORMAT, server.getId(), healthUrl);
		if (StringUtils.isNotBlank(projectName)) {
			healthCheckUrl = healthCheckUrl + "?projectName=" + projectName;
		}
		try {
			ResponseEntity<String> responseEntity = restTemplate.getForEntity(healthCheckUrl, String.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				return ErrorLevel.NORMAL;
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("health check failed, server: {}", server, e);
			} else {
				log.warn("health check failed, server: {}", server);
			}
			if (e instanceof ResourceAccessException) {
				return ErrorLevel.FATAL;
			}
			return ErrorLevel.ERROR;
		}
		return ErrorLevel.WARN;
	}

	public Double getServerLoad(Server server) {
		if (Objects.isNull(server)) {
			return MAX_LOAD;
		}
		String heathLoadUrl = String.format(HEALTH_URL_FORMAT, server.getId(), loadUrl);
		try {
			ResponseEntity<String> responseEntity = restTemplate.getForEntity(heathLoadUrl, String.class);
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				String result = responseEntity.getBody();
				JSONObject jsonObject = JSONObject.parseObject(result);
				String load = jsonObject.getString("msg");
				return Double.valueOf(load);
			}
		} catch (Exception e) {
			// Nothing to do
		}
		return MAX_LOAD;
	}

	public void generateSchema(Server server) {
		if (Objects.isNull(server)) {
			return;
		}
		String healthCheckUrl = String.format(HEALTH_URL_FORMAT, server.getId(), generateSchemaUrl);
		if (StringUtils.isBlank(projectName) || StringUtils.isBlank(syncUsername) || StringUtils.isBlank(syncPassword)) {
			return;
		}
		healthCheckUrl = healthCheckUrl + "/" + projectName;
		try {
			String dePwd = Encryptor.decrypt(syncPassword);
			String basicAuth = buildBasicAuth(syncUsername, dePwd);
			HttpEntity<String> requestEntity = getCheckDatasetHttpEntity(basicAuth);
			restTemplate.postForEntity(healthCheckUrl, requestEntity, String.class);
		} catch (Exception e) {
			log.warn("{} generate schema failed", server);
		}
	}

	public enum ErrorLevel {
		NORMAL,
		WARN,
		ERROR,
		FATAL
	}

}
