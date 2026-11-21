package com.vacationary.backend.http.features.history;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

import com.vacationary.backend.core.interfaces.http.IHttpRequestReader;
import com.vacationary.backend.dtos.http.ApiHistoryRequest;

public class HistoryRequestReader implements IHttpRequestReader<ApiHistoryRequest> {

	@Override
	@SuppressWarnings("unchecked")
	public ApiHistoryRequest read(HttpServletRequest request) {
		Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		Map<String, String> variables = attribute instanceof Map<?, ?> map
				? (Map<String, String>) map
				: Map.of();
		return new ApiHistoryRequest(variables.get("channel"), variables.get("externalUserId"));
	}
}
