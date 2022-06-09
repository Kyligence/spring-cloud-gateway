package io.kyligence.kap.gateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.cloud.gateway.support.msg.ErrorCode;
import org.springframework.cloud.gateway.support.msg.MessageUtils;
import org.springframework.cloud.gateway.support.msg.MsgPicker;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

public class KylinErrorAttributes extends DefaultErrorAttributes {

	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		Map<String, Object> msg = getErrorAttributes(request, options.isIncluded(ErrorAttributeOptions.Include.STACK_TRACE));
		if (msg != null) {
			failCode(msg);
		}
		return msg;
	}

	private void failCode(Map<String, Object> msg) {
		if (msg != null) {
			String message = (String) msg.get("message");
			if (message != null) {
				if (!MessageUtils.isFormatError(message)) {
					if (msg.get("status") != null && msg.get("status").equals(500)) {
						msg.put("stacktrace", message);
						message = MsgPicker.getMsg().getContext(ErrorCode.SYSTEM_ERROR);
					}
				}
				msg.put("msg", message);
				msg.put("code", MessageUtils.DEFAULT_ERROR_CODE);
			}
		}
	}
}
