package com.vacationary.backend.http.spring;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;

public class HttpResponseWriter implements IHttpResponseWriter {

	private final ObjectMapper objectMapper;

	public HttpResponseWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void writeJson(HttpServletResponse response, int status, Object body) {
		try {
			byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
			response.setStatus(status);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType("application/json");
			response.setContentLength(json.length);
			response.getOutputStream().write(json);
		}
		catch (Exception error) {
			throw new IllegalStateException("failed to write HTTP response", error);
		}
	}
}
