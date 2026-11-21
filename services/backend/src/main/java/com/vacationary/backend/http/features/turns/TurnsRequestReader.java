package com.vacationary.backend.http.features.turns;

import jakarta.servlet.http.HttpServletRequest;

import tools.jackson.databind.ObjectMapper;

import com.vacationary.backend.core.interfaces.http.IHttpRequestReader;
import com.vacationary.backend.dtos.http.ApiTurnRequest;
import com.vacationary.backend.errors.bl.BlValidationError;

public class TurnsRequestReader implements IHttpRequestReader<ApiTurnRequest> {

	private final ObjectMapper objectMapper;

	public TurnsRequestReader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public ApiTurnRequest read(HttpServletRequest request) {
		try {
			byte[] body = request.getInputStream().readAllBytes();
			if (body.length == 0) {
				throw new BlValidationError("request body is required");
			}
			ApiTurnRequest parsed = objectMapper.readValue(body, ApiTurnRequest.class);
			if (parsed == null) {
				throw new BlValidationError("request body is required");
			}
			return parsed;
		}
		catch (BlValidationError error) {
			throw error;
		}
		catch (Exception error) {
			throw new BlValidationError("request body is invalid");
		}
	}
}
