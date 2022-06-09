package io.kyligence.kap.gateway.route.reader;

import com.google.common.collect.Lists;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.config.MdxConfig;
import io.kyligence.kap.gateway.constant.KylinGatewayVersion;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.persistent.domain.MdxRouteDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ConfigRouteTableReader implements IRouteTableReader {

	@Autowired
	private MdxConfig mdxConfig;

	@Override
	public List<KylinRouteRaw> list() {
		List<KylinRouteRaw> kylinRouteRawList = Lists.newArrayList();
		List<MdxConfig.ProxyInfo> proxyInfos = mdxConfig.getProxy();
		for (MdxConfig.ProxyInfo proxyInfo : proxyInfos) {
			if (!KylinGatewayVersion.MDX.equals(proxyInfo.getType())) {
				continue;
			}
			if (CollectionUtils.isEmpty(proxyInfo.getServers())) {
				log.error("The server list is null, please check it!");
				return kylinRouteRawList;
			}
			List<Server> servers = proxyInfo.getServers().stream().map(Server::new).collect(Collectors.toList());
			if (StringUtils.isNotBlank(proxyInfo.getHost())) {
				String host = proxyInfo.getHost();
				kylinRouteRawList.add(toRouteRaw(proxyInfo, host, servers));
			}
			if (CollectionUtils.isNotEmpty(proxyInfo.getHosts())) {
				for (String host : proxyInfo.getHosts()) {
					kylinRouteRawList.add(toRouteRaw(proxyInfo, host, servers));
				}
			}
		}
		return kylinRouteRawList;
	}

	private KylinRouteRaw toRouteRaw(MdxConfig.ProxyInfo proxyInfo, String host, List<Server> servers) {
		String[] tmp = host.split(":");
		if (tmp.length < 2) {
			// from test.com to test.com:80
			host = tmp[0] + ":80";
		}
		MdxRouteDO mdxRouteDO = new MdxRouteDO(proxyInfo.getType(), proxyInfo.getRoute(), host, servers);
		return KylinRouteRaw.convert(mdxRouteDO);
	}

}
