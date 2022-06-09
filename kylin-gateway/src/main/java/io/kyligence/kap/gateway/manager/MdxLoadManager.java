package io.kyligence.kap.gateway.manager;

import io.kyligence.kap.gateway.bean.ServerInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liang.xu
 */
@Slf4j
@Component
public class MdxLoadManager {

	/**
	 * key: server
	 * value: loadInfo
	 */
	public static final Map<String, LoadInfo> LOAD_INFO_MAP = new ConcurrentHashMap<>();

	private static Long cacheTime;

	private static Long serverSize;

	private static Double memoryWeight;

	private static Double queryWeight;

	private static Double threshold;

	@Autowired
	private ServiceManager serviceManager;

	public void updateServerByMemLoad(String serverId, double memLoad) {
		if (StringUtils.isBlank(serverId)) {
			return;
		}

		if (memLoad > threshold) {
			for (Map.Entry<String, ServerInfo> entry :
					serviceManager.serverMap.entrySet()) {
				ServerInfo serverInfo = entry.getValue();
				if (serverInfo == null) {
					continue;
				}
				if (serverId.equals(serverInfo.getServer())) {
					serviceManager.serverMap.remove(entry.getKey());
				}
			}
		}
		LoadInfo loadInfo = LOAD_INFO_MAP.get(serverId);
		if (loadInfo == null) {
			loadInfo = new LoadInfo(0D, 0D, 0D);
		}
		double nodeLoad = memLoad * memoryWeight + loadInfo.getQueryLoad() / serverSize * queryWeight;
		loadInfo.setMemLoad(memLoad);
		loadInfo.setNodeLoad(nodeLoad);
		LOAD_INFO_MAP.put(serverId, loadInfo);
	}

	public void updateServerByQueryNum(String serverId, double queryNum) {
		if (StringUtils.isBlank(serverId)) {
			return;
		}
		LoadInfo loadInfo = LOAD_INFO_MAP.get(serverId);
		if (loadInfo == null) {
			loadInfo = new LoadInfo(0D, 0D, 0D);
		}
		queryNum = loadInfo.getQueryLoad() + queryNum;
		if (queryNum < 0) {
			return;
		}
		double nodeLoad = loadInfo.getMemLoad() * memoryWeight + queryNum / serverSize * queryWeight;
		loadInfo.setQueryLoad(queryNum);
		loadInfo.setNodeLoad(nodeLoad);
		LOAD_INFO_MAP.put(serverId, loadInfo);
	}

	public void removeServer(String serverKey) {
		LOAD_INFO_MAP.remove(serverKey);
		for (Map.Entry<String, ServerInfo> entry : serviceManager.serverMap.entrySet()) {
			ServerInfo serverInfo = entry.getValue();
			if (serverInfo == null) {
				continue;
			}
			if (serverKey.equals(serverInfo.getServer())) {
				serviceManager.serverMap.remove(entry.getKey());
			}
		}
		log.info("removed server: {} from active server list", serverKey);
	}

	public static long getCacheTime() {
		return cacheTime;
	}

	@Value(value = "${mdx.cache-time}")
	public void setCacheTime(long cacheTime) {
		MdxLoadManager.cacheTime = cacheTime;
	}

	@Value(value = "${mdx.server-size}")
	public void setServerSize(long serverSize) {
		MdxLoadManager.serverSize = serverSize;
	}

	@Value(value = "${mdx.weight.memory}")
	public void setMemWeight(Double memWeight) {
		MdxLoadManager.memoryWeight = memWeight;
	}

	@Value(value = "${mdx.weight.query}")
	public void setQueryWeight(Double queryWeight) {
		MdxLoadManager.queryWeight = queryWeight;
	}

	@Value("${mdx.weight.threshold:0.9}")
	private void setThreshold(double threshold) {
		MdxLoadManager.threshold = threshold;
	}

	@Data
	@AllArgsConstructor
	public static class LoadInfo {

		private Double memLoad;

		private Double queryLoad;

		private Double nodeLoad;
	}

}
