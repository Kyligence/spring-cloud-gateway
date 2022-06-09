package io.kyligence.kap.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
@ConfigurationProperties(prefix = "mdx")
public class MdxConfig {

	List<ProxyInfo> proxy;

	public void setProxyInfo(List<ProxyInfo> proxyInfos) {
		this.proxy = proxyInfos;
	}

	@Data
	public static class ProxyInfo {

		private String type;

		private String route;

		private String host;

		private List<String> hosts;

		private List<String> servers;

	}

}
