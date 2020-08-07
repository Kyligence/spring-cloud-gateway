package io.kyligence.kap.gateway.event;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;

public class Kylin3XRefreshRoutesEvent extends RefreshRoutesEvent {

	/**
	 * Create a new ApplicationEvent.
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public Kylin3XRefreshRoutesEvent(Object source) {
		super(source);
	}

}
