package io.kyligence.kap.gateway.event;

import com.google.common.collect.Sets;
import com.netflix.loadbalancer.Server;
import lombok.Getter;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;

import java.util.Objects;
import java.util.Set;

public class KylinRefreshRoutesEvent extends RefreshRoutesEvent {

	@Getter
	private final Set<Server> serverSet;

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public KylinRefreshRoutesEvent(Object source) {
		this(source, null);
	}

	public KylinRefreshRoutesEvent(Object source, Set<Server> serverSet) {
		super(source);
		this.serverSet = Objects.isNull(serverSet) ? Sets.newHashSet() : serverSet;
	}
}
