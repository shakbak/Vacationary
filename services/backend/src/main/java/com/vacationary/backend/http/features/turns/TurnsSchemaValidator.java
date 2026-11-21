package com.vacationary.backend.http.features.turns;

import com.vacationary.backend.core.interfaces.http.ISchemaValidator;
import com.vacationary.backend.dtos.http.ApiTurnRequest;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.http.spring.HttpChannels;

public class TurnsSchemaValidator implements ISchemaValidator<ApiTurnRequest> {

	@Override
	public void validate(ApiTurnRequest request) {
		if (request == null) {
			throw new BlValidationError("request body is required");
		}
		if (HttpChannels.trimToNull(request.channel()) == null) {
			throw new BlValidationError("channel is required");
		}
		if (!HttpChannels.isSupported(request.channel())) {
			throw new BlValidationError("channel is invalid");
		}
		if (HttpChannels.trimToNull(request.externalUserId()) == null) {
			throw new BlValidationError("externalUserId is required");
		}
		if (HttpChannels.trimToNull(request.text()) == null) {
			throw new BlValidationError("text is required");
		}
		if (HttpChannels.trimToNull(request.eventId()) == null) {
			throw new BlValidationError("eventId is required");
		}
	}
}
