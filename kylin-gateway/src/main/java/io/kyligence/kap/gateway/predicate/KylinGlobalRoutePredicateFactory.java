package io.kyligence.kap.gateway.predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.utils.UrlProjectUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PROJECTS_KEY;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PROJECT_FLAG;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PROJECT_KEY;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.PROJECT_NO_RESOURCE_GROUP_EXCEPTION;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.READ_REQUEST_BODY_OBJECT_KEY;

public class KylinGlobalRoutePredicateFactory extends AbstractRoutePredicateFactory<KylinGlobalRoutePredicateFactory.Config> {
	private static final Log log = LogFactory.getLog(KylinGlobalRoutePredicateFactory.class);

	private final List<HttpMessageReader<?>> messageReaders;

	private final Class inClass;

	public KylinGlobalRoutePredicateFactory() {
		super(KylinGlobalRoutePredicateFactory.Config.class);
		this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
		this.inClass = String.class;
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Lists.newArrayList(PROJECTS_KEY);
	}

	@Override
	public ShortcutType shortcutType() {
		return ShortcutType.GATHER_LIST;
	}

	private void setAttribute(ServerWebExchange exchange, String key, String value) {
		exchange.getAttributes().put(key, value);
	}

	private String readProjectFromCacheBody(String cacheBody) {
		if (StringUtils.isBlank(cacheBody)) {
			return null;
		}

		try {
			HashMap json = new ObjectMapper().readValue(cacheBody, HashMap.class);
			if (null == json) {
				return null;
			}

			Optional jsonKey = json.keySet().stream()
					.filter(key -> PROJECT_KEY.equalsIgnoreCase(key.toString()))
					.findFirst();

			if (jsonKey.isPresent()) {
				return (String) json.get(jsonKey.get());
			}
		} catch (Exception e) {
			log.error("Failed to read project from cache body!", e);
		}

		return null;
	}

	private String readProjectFromCacheBody(Object cacheBody) {
		Preconditions.checkNotNull(cacheBody);
		return readProjectFromCacheBody(cacheBody.toString());
	}

	private String getProjectFromProjectList(List<String> projects) {
		if (CollectionUtils.isEmpty(projects)) {
			return null;
		}

		if (StringUtils.isBlank(projects.get(0))) {
			return null;
		}

		return projects.get(0);
	}

	private Mono<Boolean> setProjectNoResourceGroupException(ServerWebExchange exchange, String project) {
		exchange.getAttributes().put(PROJECT_NO_RESOURCE_GROUP_EXCEPTION, "true");
		setAttribute(exchange, PROJECT_KEY, project);
		return Mono.just(true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public AsyncPredicate<ServerWebExchange> applyAsync(KylinGlobalRoutePredicateFactory.Config config) {
		return new AsyncPredicate<ServerWebExchange>() {
			@Override
			public Publisher<Boolean> apply(ServerWebExchange exchange) {
				if (Objects.nonNull(exchange.getAttribute(PROJECT_FLAG))) {
					if (Objects.isNull(exchange.getAttribute(PROJECT_KEY))) {
						return Mono.just(true);
					}

					return setProjectNoResourceGroupException(exchange, exchange.getAttribute(PROJECT_KEY));
				}

				if (Boolean.valueOf(exchange.getAttribute(READ_REQUEST_BODY_OBJECT_KEY))) {
					return Mono.just(true);
				}

				if (exchange.getRequest().getMethod() == HttpMethod.GET) {
					return Mono.just(true);
				}

				String headerProject = getProjectFromProjectList(exchange.getRequest().getHeaders().get(PROJECT_KEY));
				if (Objects.nonNull(headerProject)) {
					return setProjectNoResourceGroupException(exchange, headerProject);
				}

				String queryProject = getProjectFromProjectList(exchange.getRequest().getQueryParams().get(PROJECT_KEY));
				if (Objects.nonNull(queryProject)) {
					return setProjectNoResourceGroupException(exchange, queryProject);
				}

				String pathProject = UrlProjectUtil.extractProjectFromUrlPath(exchange);
				if (StringUtils.isNotBlank(pathProject)) {
					return setProjectNoResourceGroupException(exchange, pathProject);
				}

				exchange.getAttributes().put(READ_REQUEST_BODY_OBJECT_KEY, "true");
				return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange,
						serverHttpRequest -> ServerRequest
								.create(exchange.mutate().request(serverHttpRequest).build(), messageReaders)
								.bodyToMono(inClass)
								.switchIfEmpty(Mono.just(true))
								.map(objectValue -> {
									String project = readProjectFromCacheBody(objectValue);
									if (Objects.nonNull(project)) {
										setProjectNoResourceGroupException(exchange, project);
									}
									return Objects.isNull(project) ? "" : project;
								}).map(project -> true));
			}

			@Override
			public String toString() {
				return String.format("KylinGlobal: %s", Arrays.toString(config.getProjects().toArray()));
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate<ServerWebExchange> apply(KylinGlobalRoutePredicateFactory.Config config) {
		throw new UnsupportedOperationException("KylinGlobalRoutePredicateFactory is only async.");
	}

	@Validated
	public static class Config {

		private List<String> projects = Lists.newArrayList();

		public List<String> getProjects() {
			return projects;
		}

		public void setProjects(List<String> projects) {
			this.projects = projects;
		}

	}
}
