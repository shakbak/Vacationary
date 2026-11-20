package com.vacationary.backend.dtos.assistant;

import java.util.List;

public record BlAssistantRequest(List<BlChatMessage> messages, String latestUserText) {

	public BlAssistantRequest {
		messages = messages == null ? List.of() : List.copyOf(messages);
	}
}
