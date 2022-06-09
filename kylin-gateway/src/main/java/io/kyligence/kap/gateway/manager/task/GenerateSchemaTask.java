package io.kyligence.kap.gateway.manager.task;

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.Server;
import io.kyligence.kap.gateway.loadbalancer.MdxPing;

public class GenerateSchemaTask implements Runnable {

	private final IPing ping;

	private final Server server;

	public GenerateSchemaTask(IPing ping, Server server) {
		this.ping = ping;
		this.server = server;
	}

	@Override
	public void run() {
		if (ping instanceof MdxPing) {
			((MdxPing) ping).generateSchema(server);
		}
	}

}
