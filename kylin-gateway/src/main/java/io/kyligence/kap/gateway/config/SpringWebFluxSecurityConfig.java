package io.kyligence.kap.gateway.config;

import com.google.common.collect.Lists;

import java.nio.charset.Charset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.StringUtils;

/**
 * @author xiuzhao.wu
 * @date 2023/3/22 22:38
 */
@ConditionalOnProperty(name = "kylin.gateway.auth.enable", havingValue = "true")
@EnableConfigurationProperties(SecurityProperties.class)
@EnableWebFluxSecurity
public class SpringWebFluxSecurityConfig {

	@Value("${kylin.gateway.auth.username:ADMIN}")
	private String username;

	@Value("${kylin.gateway.auth.password:KYLIN}")
	private String password;

	@Value("${kylin.gateway.auth.pattern:/actuator/**}")
	private String authUrl;

	@Bean
	public SecurityWebFilterChain actuatorSecurityFilterChain(ServerHttpSecurity http) {
		final String[] patterns = StringUtils.commaDelimitedListToStringArray(authUrl);
		http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(patterns))
				.authorizeExchange()
				.anyExchange().authenticated()
				.and()
				.httpBasic().and()
				.logout().disable()
				.formLogin().disable()
				.anonymous().disable()
				.csrf().disable();
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder passwordEncoder) {
		if (password == null) {
            throw new IllegalArgumentException("password cannot be null");
        }
        if (password.getBytes(Charset.defaultCharset()).length > 72) {
            throw new IllegalArgumentException("password cannot be more than 72 bytes");
        }

		final UserDetails userDetails = User.withUsername(username)
				.password(passwordEncoder.encode(password))
				.roles(StringUtils.toStringArray(Lists.newArrayList("ADMIN")))
				.build();
		return new MapReactiveUserDetailsService(userDetails);
	}

}
