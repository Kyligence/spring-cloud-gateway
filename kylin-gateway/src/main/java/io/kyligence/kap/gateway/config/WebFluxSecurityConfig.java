package io.kyligence.kap.gateway.config;

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * @author xiuzhao.wu
 * @date 2023/3/22 13:34
 */
@ConditionalOnProperty(name = "kylin.gateway.auth.enable", havingValue = "true")
@Import({ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class,
		ReactiveManagementWebSecurityAutoConfiguration.class})
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
		httpSecurity.authorizeExchange()
				.matchers(EndpointRequest.toAnyEndpoint()).authenticated()
				.anyExchange().permitAll()
				.and()
				.httpBasic()
				.and()
				.formLogin().disable()
				.logout().disable()
				.csrf().disable();
		return httpSecurity.build();
	}

}
