package com.vacationary.backend.dtos.http;

import java.util.List;

public record ApiConversationResponse(List<ApiMessage> messages) {

	public ApiConversationResponse {
		messages = messages == null ? List.of() : List.copyOf(messages);
	}
}
