package com.vacationary.backend.core.interfaces.http;

import jakarta.servlet.http.HttpServletResponse;

public interface IResponseHandler<TResult> {

	void handle(TResult result, HttpServletResponse response);
}
