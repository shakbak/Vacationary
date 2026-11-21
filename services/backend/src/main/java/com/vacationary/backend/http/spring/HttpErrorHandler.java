package com.vacationary.backend.http.spring;

import jakarta.servlet.http.HttpServletResponse;

import com.vacationary.backend.constants.HttpStatuses;
import com.vacationary.backend.core.interfaces.http.IErrorHandler;
import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.errors.http.ApiErrorBody;

public class HttpErrorHandler implements IErrorHandler {

	static final String INTERNAL_CODE = "INTERNAL_ERROR";
	static final String INTERNAL_MESSAGE = "An unexpected error occurred";

	private final IHttpResponseWriter responseWriter;

	public HttpErrorHandler(IHttpResponseWriter responseWriter) {
		this.responseWriter = responseWriter;
	}

	@Override
	public void handle(Exception error, HttpServletResponse response) {
		if (error instanceof BlValidationError validation) {
			responseWriter.writeJson(
					response,
					HttpStatuses.BAD_REQUEST,
					new ApiErrorBody(validation.getCode(), validation.getMessage()));
			return;
		}
		responseWriter.writeJson(
				response,
				HttpStatuses.INTERNAL_SERVER_ERROR,
				new ApiErrorBody(INTERNAL_CODE, INTERNAL_MESSAGE));
	}
}
