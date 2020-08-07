package io.kyligence.kap.gateway.filter;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

public class KylinRedirectToGatewayFilter implements GlobalFilter, Ordered {

	private static final Logger logger = LoggerFactory
			.getLogger(KylinRedirectToGatewayFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		final ServerHttpResponse response = exchange.getResponse();
		final URI uri = exchange.getRequest().getURI();

		ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(
				response) {
			@SuppressWarnings("serial")
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
				if (null != getDelegate().getStatusCode()
						&& getDelegate().getStatusCode().is3xxRedirection()) {
					HttpHeaders headers = getDelegate().getHeaders();
					String location = headers.getFirst(HttpHeaders.LOCATION);

					int i = -1;
					if (StringUtils.isEmpty(location)) {
						logger.error("Can not redirect URI, cause by location is empty!");
					}
					else if ((i = location.indexOf('/', 8)) < 0) {
						// redirect URI must be have sub url, like
						// http://authoriy/kylin/api/xxxx
						logger.error("Can not redirect URI, cause by location: {}",
								location);
					}
					else {
						try {
							String redirectLocation = String.format("%s://%s%s",
									uri.getScheme(), uri.getAuthority(),
									location.substring(i));
							logger.debug("Redirect URL :{} to {} ", location,
									redirectLocation);
							headers.put(HttpHeaders.LOCATION,
									Lists.newArrayList(redirectLocation));
						}
						catch (Exception e) {
							logger.error("Failed to redirect location: {}", location, e);
						}
					}
				}

				return super.writeWith(body);
			}
		};
		// replace response with decorator
		return chain.filter(exchange.mutate().response(decoratedResponse).build());
	}

	@Override
	public int getOrder() {
		return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
	}

}
