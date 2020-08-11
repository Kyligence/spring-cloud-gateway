/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kyligence.kap.gateway;

import com.netflix.loadbalancer.IPingStrategy;
import io.kyligence.kap.gateway.health.ConcurrentPingStrategy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author zhiyu.zeng
 */

@SpringBootApplication
public class BootstrapServer {
	public static void main(String[] args) {
		SpringApplication.run(BootstrapServer.class, args);
	}

	@Bean
	@ConfigurationProperties(prefix = "kylin.gateway.health.rest-template")
	public HttpComponentsClientHttpRequestFactory httpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory();
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(httpRequestFactory());
		return restTemplate;
	}

	@Bean
	@ConfigurationProperties(prefix = "kylin.gateway.health.ping-strategy")
	public IPingStrategy pingStrategy() {
		return new ConcurrentPingStrategy();
	}

}
