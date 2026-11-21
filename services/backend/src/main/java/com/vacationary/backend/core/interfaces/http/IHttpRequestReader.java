package com.vacationary.backend.core.interfaces.http;

import jakarta.servlet.http.HttpServletRequest;

public interface IHttpRequestReader<TRequest> {

	TRequest read(HttpServletRequest request);
}
