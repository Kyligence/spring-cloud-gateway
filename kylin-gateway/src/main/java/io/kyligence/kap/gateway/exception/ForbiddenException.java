package io.kyligence.kap.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ForbiddenException extends ResponseStatusException {

	public ForbiddenException(String message) {
		this(HttpStatus.FORBIDDEN, message);
	}

	public ForbiddenException(String message, Throwable cause) {
		this(HttpStatus.FORBIDDEN, message, cause);
	}

	private ForbiddenException(HttpStatus httpStatus, String message) {
		super(httpStatus, message);
	}

	private ForbiddenException(HttpStatus httpStatus, String message, Throwable cause) {
		super(httpStatus, message, cause);
	}

	public static ForbiddenException create(String message) {
		return new ForbiddenException(HttpStatus.FORBIDDEN, message);
	}

	public static ForbiddenException create(String message, Throwable cause) {
		return new ForbiddenException(HttpStatus.FORBIDDEN, message, cause);
	}

}
