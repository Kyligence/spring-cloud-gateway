package io.kyligence.kap.gateway.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class Response<T> {

	private Integer status;

	@JsonInclude
	private T data;

	private String errorMsg;

	public Response(T data) {
		this.status = Status.SUCCESS.ordinal();
		this.data = data;
	}

	public Response(Status status) {
		this.status = status.ordinal();
	}

	public Response(Status status, T data) {
		this.status = status.ordinal();
		this.data = data;
	}

	public Response<T> data(T data) {
		this.data = data;
		return this;
	}

	public Response<T> errorMsg(String errorMesg) {
		this.errorMsg = errorMesg;
		return this;
	}

	public enum Status {

		SUCCESS,

		FAIL

	}

}
