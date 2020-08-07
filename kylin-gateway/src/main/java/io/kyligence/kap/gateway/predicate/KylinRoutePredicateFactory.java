package io.kyligence.kap.gateway.predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public class KylinRoutePredicateFactory
		extends AbstractRoutePredicateFactory<KylinRoutePredicateFactory.Config> {

	private static final Log log = LogFactory.getLog(KylinRoutePredicateFactory.class);

	private static final String PROJECT_KEY = "project";

	private static final String PROJECTS_KEY = "projects";

	private static final String TEST_ATTRIBUTE = "read_body_predicate_test_attribute";

	private static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";

	private final List<HttpMessageReader<?>> messageReaders;

	private final Class inClass;

	public KylinRoutePredicateFactory() {
		super(Config.class);
		this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
		this.inClass = String.class;
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(PROJECTS_KEY);
	}

	@Override
	public ShortcutType shortcutType() {
		return ShortcutType.GATHER_LIST;
	}

	private boolean testBasic(List<String> projects, Config config) {
		if (projects.isEmpty()) {
			return false;
		}

		boolean match = false;
		for (String project : config.getProjects()) {
			match = projects.stream().anyMatch(value -> value.equalsIgnoreCase(project));
			if (match) {
				return true;
			}
		}

		return match;
	}

	private boolean testHeader(ServerWebExchange exchange, Config config) {
		List<String> values = exchange.getRequest().getHeaders().getOrDefault(PROJECT_KEY,
				Collections.emptyList());
		return testBasic(values, config);
	}

	private boolean testQuery(ServerWebExchange exchange, Config config) {
		List<String> values = exchange.getRequest().getQueryParams()
				.getOrDefault(PROJECT_KEY, Collections.emptyList());
		return testBasic(values, config);
	}

	private Predicate<String> testBodyPredicate(Config config) {
		return r -> {
			try {
				HashMap json = new ObjectMapper().readValue(r, HashMap.class);
				for (Object key : json.keySet()) {
					if (key instanceof String
							&& PROJECT_KEY.equalsIgnoreCase((String) key)) {

						for (String project : config.getProjects())
							if (project.equalsIgnoreCase((String) json.get(key))) {
								return true;
							}
					}
				}
			}
			catch (Exception e) {
				log.error("Failed to check project from body!", e);
			}

			return false;
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public AsyncPredicate<ServerWebExchange> applyAsync(Config config) {
		Predicate predicate = testBodyPredicate(config);

		return new AsyncPredicate<ServerWebExchange>() {
			@Override
			public Publisher<Boolean> apply(ServerWebExchange exchange) {
				Object cachedBody = exchange.getAttribute(CACHE_REQUEST_BODY_OBJECT_KEY);

				if (cachedBody != null) {
					try {
						boolean test = predicate.test(cachedBody);
						exchange.getAttributes().put(TEST_ATTRIBUTE, test);
						return Mono.just(test);
					}
					catch (ClassCastException e) {
						if (log.isDebugEnabled()) {
							log.debug("Predicate test failed because class in predicate "
									+ "does not match the cached body object", e);
						}
					}
					return Mono.just(false);
				}
				else {
					if (testHeader(exchange, config) || testQuery(exchange, config)) {
						return Mono.just(true);
					}

					return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange,
							(serverHttpRequest) -> ServerRequest
									.create(exchange.mutate().request(serverHttpRequest)
											.build(), messageReaders)
									.bodyToMono(inClass)
									.doOnNext(objectValue -> exchange.getAttributes().put(
											CACHE_REQUEST_BODY_OBJECT_KEY, objectValue))
									.map(objectValue -> predicate.test(objectValue)));
				}
			}

			@Override
			public String toString() {
				return String.format("Projects: %s",
						Arrays.toString(config.getProjects().toArray()));
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate<ServerWebExchange> apply(Config config) {
		throw new UnsupportedOperationException(
				"KylinRoutePredicateFactory is only async.");
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
