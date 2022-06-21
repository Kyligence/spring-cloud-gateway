package io.kyligence.kap.gateway.manager.task;

import io.kyligence.kap.gateway.bean.ServerInfo;
import io.kyligence.kap.gateway.manager.MdxLoadManager;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ClearExpiredServerTask implements Runnable {

	private static final Logger monitorLog = LoggerFactory.getLogger("monitor");

	private static final double REALLOCATE_NUM = 1.2;

	public final Map<String, ServerInfo> serverMap;

	public ClearExpiredServerTask(Map<String, ServerInfo> serverMap) {
		this.serverMap = serverMap;
	}

	@Override
	public void run() {
		try {
			cleanExpireCache();
			printMonitorInfo();
			reallocateServerCache();
		} catch (Exception e) {
			log.error("schedule catch exception: ", e);
		}
	}

	/**
	 * clean expired server which not used in two period
	 */
	private void cleanExpireCache() {
		for (Map.Entry<String, ServerInfo> entry : serverMap.entrySet()) {
			ServerInfo serverInfo = entry.getValue();
			if (serverInfo.getUpdateTime() - serverInfo.getStartTime() > MdxLoadManager.getCacheTime()) {
				serverMap.remove(serverInfo.getServer());
			}
		}
	}

	/**
	 * print route cache and cluster load information
	 */
	private void printMonitorInfo() {
		monitorLog.info("--------- server route cache info start ----------");
		for (Map.Entry<String, ServerInfo> entry : serverMap.entrySet()) {
			ServerInfo serverInfo = entry.getValue();
			if (serverInfo != null) {
				monitorLog.info(" cache key: {} route to {}", entry.getKey(), serverInfo.getServer());
			}
		}
		monitorLog.info("--------- server route cache info end ----------");

		monitorLog.info("--------- cluster load info start ----------");
		for (Map.Entry<String, MdxLoadManager.LoadInfo> entry : MdxLoadManager.LOAD_INFO_MAP.entrySet()) {
			MdxLoadManager.LoadInfo loadInfo = entry.getValue();
			if (loadInfo != null) {
				monitorLog.info(" node server is : {},  load is: {}, memory load is: {}, query num is: {}",
						entry.getKey(), loadInfo.getNodeLoad(), loadInfo.getMemLoad(),
						loadInfo.getQueryLoad());
			}
		}
		monitorLog.info("--------- cluster load info end ----------");
	}

	/**
	 * avoid query store in one node
	 */
	private void reallocateServerCache() {
		Map<String, Double> tmp = new HashMap<>();
		double querySum = 0D;
		for (Map.Entry<String, MdxLoadManager.LoadInfo> entry : MdxLoadManager.LOAD_INFO_MAP.entrySet()) {
			MdxLoadManager.LoadInfo loadInfo = entry.getValue();
			if (loadInfo != null) {
				querySum += loadInfo.getQueryLoad();
				tmp.put(entry.getKey(), loadInfo.getQueryLoad());
			}
		}

		double avgQuery = querySum / MdxLoadManager.LOAD_INFO_MAP.size();
		for (Map.Entry<String, Double> entry : tmp.entrySet()) {
			if (entry.getValue() / avgQuery > REALLOCATE_NUM) {
				tmp.put(entry.getKey(), entry.getValue() - avgQuery);
			}
		}

		for (Map.Entry<String, ServerInfo> entry : serverMap.entrySet()) {
			ServerInfo serverInfo = entry.getValue();
			if (serverInfo == null) {
				continue;
			}
			String serverId = serverInfo.getServer();
			double needRemoveNum = tmp.getOrDefault(serverId, 0D);
			if (needRemoveNum < 1) {
				continue;
			}
			serverMap.remove(entry.getKey());
			needRemoveNum--;
			tmp.put(serverId, needRemoveNum);
		}
	}

}
