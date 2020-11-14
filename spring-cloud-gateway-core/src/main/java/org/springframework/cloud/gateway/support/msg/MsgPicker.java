package org.springframework.cloud.gateway.support.msg;

import org.springframework.web.server.ServerWebExchange;

import java.util.List;

public class MsgPicker {

	private static ThreadLocal<Message> msg = new ThreadLocal<>();

	public static void setMsg(ServerWebExchange exchange) {
		List<String> lang = exchange.getRequest().getHeaders().get("Accept-Language");
		if (null != lang && lang.size() > 0) {
			setMsg(lang.get(0));
		}
	}

	public static void setMsg(String lang) {
		if ("cn".equals(lang))
			msg.set(CnMessage.getInstance());
		else
			msg.set(Message.getInstance());
	}

	public static Message getMsg() {
		Message ret = msg.get();
		if (ret == null) { // use English by default
			ret = Message.getInstance();
			msg.set(Message.getInstance());
		}
		return ret;
	}
}
