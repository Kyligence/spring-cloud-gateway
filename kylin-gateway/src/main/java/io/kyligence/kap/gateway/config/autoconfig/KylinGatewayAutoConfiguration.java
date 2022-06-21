package io.kyligence.kap.gateway.config.autoconfig;

import io.kyligence.kap.gateway.exception.KylinErrorAttributes;
import io.kyligence.kap.gateway.filter.KylinRedirectToGatewayFilter;
import io.kyligence.kap.gateway.filter.MdxLoadMonitorFilter;
import io.kyligence.kap.gateway.manager.MdxLoadManager;
import io.kyligence.kap.gateway.predicate.KylinGlobalRoutePredicateFactory;
import io.kyligence.kap.gateway.predicate.KylinRoutePredicateFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
@EnableConfigurationProperties
@AutoConfigureBefore({HttpHandlerAutoConfiguration.class,
		WebFluxAutoConfiguration.class})
@AutoConfigureAfter({GatewayLoadBalancerClientAutoConfiguration.class,
		GatewayClassPathWarningAutoConfiguration.class})
@ConditionalOnClass(DispatcherHandler.class)
public class KylinGatewayAutoConfiguration {

	@Bean
	public KylinRoutePredicateFactory kylinRoutePredicateFactory() {
		return new KylinRoutePredicateFactory();
	}

	@Bean
	public KylinGlobalRoutePredicateFactory kylinGlobalRoutePredicateFactory() {
		return new KylinGlobalRoutePredicateFactory();
	}

	@Bean
	public DefaultErrorAttributes errorAttributes() {
		return new KylinErrorAttributes();
	}

	@Bean
	@ConditionalOnProperty(name = "server.type", havingValue = "ke")
	public KylinRedirectToGatewayFilter kylinRedirectToGatewayFilter() {
		return new KylinRedirectToGatewayFilter();
	}

}
