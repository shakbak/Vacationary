package com.vacationary.backend.http.features.history;

import com.vacationary.backend.core.interfaces.http.ISchemaValidator;
import com.vacationary.backend.dtos.http.ApiHistoryRequest;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.http.spring.HttpChannels;

public class HistorySchemaValidator implements ISchemaValidator<ApiHistoryRequest> {

	@Override
	public void validate(ApiHistoryRequest request) {
		if (request == null) {
			throw new BlValidationError("request is required");
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
	}
}
