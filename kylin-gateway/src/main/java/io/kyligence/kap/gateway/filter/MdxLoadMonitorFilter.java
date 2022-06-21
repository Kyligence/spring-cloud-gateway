package io.kyligence.kap.gateway.filter;

import io.kyligence.kap.gateway.manager.MdxLoadManager;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @author liang.xu
 */

@Slf4j
public class MdxLoadMonitorFilter implements GlobalFilter, Ordered {

	public final static String MDX_QUERY_ID = "Mdx-Query-Id";

	private final MdxLoadManager mdxLoadManager;

	public MdxLoadMonitorFilter(MdxLoadManager mdxLoadManager) {
		this.mdxLoadManager = mdxLoadManager;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpResponse originalResponse = exchange.getResponse();
		ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
				URI uri = (URI) exchange.getAttributes().get(GATEWAY_REQUEST_URL_ATTR);
				if (uri != null) {
					HttpHeaders httpHeaders = originalResponse.getHeaders();
					List<String> params = httpHeaders.get(MDX_QUERY_ID);
					String serverId = uri.getAuthority();
					if (params != null) {
						String queryId = params.get(0);
						log.info("MDX query id: {} send to node: {}", queryId, serverId);
					}
					mdxLoadManager.updateServerByQueryNum(serverId, -1);
				}
				return super.writeWith(body);
			}
		};
		return chain.filter(exchange.mutate().response(decoratedResponse).build());
	}

	@Override
	public int getOrder() {
		return -2;
	}
}
