package io.kyligence.kap.gateway.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerInfo {

	private String server;

	private long startTime;

	private long updateTime;

}
