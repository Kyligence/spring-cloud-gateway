package io.kyligence.kap.gateway.config.autoconfig;

import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import io.kyligence.kap.gateway.filter.KylinLoadBalancerClientFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

@ConditionalOnProperty(name = "server.type", havingValue = KylinGatewayVersion.KYLIN_4X)
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({LoadBalancerClient.class, RibbonAutoConfiguration.class,
		DispatcherHandler.class})
@AutoConfigureAfter(RibbonAutoConfiguration.class)
@EnableConfigurationProperties(LoadBalancerProperties.class)
public class GatewayKylinLoadBalancerClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({LoadBalancerClientFilter.class,
			ReactiveLoadBalancerClientFilter.class})
	public LoadBalancerClientFilter loadBalancerClientFilter(LoadBalancerClient client,
															 LoadBalancerProperties properties) {
		return new KylinLoadBalancerClientFilter(client, properties);
	}
}
