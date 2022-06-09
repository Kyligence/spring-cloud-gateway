package io.kyligence.kap.gateway.config;

import com.google.common.collect.ImmutableList;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Scope(value = "singleton")
@Component
public class GlobalConfig {

	@Getter
	private final AtomicLong lastValidRawRouteTableMvcc = new AtomicLong(0);

	@Value(value = "${mdx.ping-strategy.interval-seconds:3}")
	private long refreshInterval;

	@Getter
	private ImmutableList<KylinRouteRaw> lastValidRawRouteTable = ImmutableList.of();

	public long getRouteRefreshIntervalSeconds() {
		return refreshInterval;
	}

	public void setLastValidRawRouteTable(Collection<KylinRouteRaw> rawRouteTable) {
		this.lastValidRawRouteTable = ImmutableList.copyOf(rawRouteTable);
	}
}
