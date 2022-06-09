package io.kyligence.kap.gateway.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyConfig {

	private Proxy mdx;

	@Data
	public static class Proxy {

		List<MdxConfig.ProxyInfo> proxy;

	}

}
