package io.kyligence.kap.gateway.event;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;

public class KylinRefreshRoutesEvent extends RefreshRoutesEvent {

	/**
	 * Create a new ApplicationEvent.
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public KylinRefreshRoutesEvent(Object source) {
		super(source);
	}

}
