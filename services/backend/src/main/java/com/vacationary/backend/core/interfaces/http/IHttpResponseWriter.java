package com.vacationary.backend.core.interfaces.http;

import jakarta.servlet.http.HttpServletResponse;

public interface IHttpResponseWriter {

	void writeJson(HttpServletResponse response, int status, Object body);
}
