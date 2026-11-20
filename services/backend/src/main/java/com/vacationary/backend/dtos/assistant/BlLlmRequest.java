package com.vacationary.backend.dtos.assistant;

import java.util.List;

public record BlLlmRequest(
		String systemPrompt,
		List<BlLlmMessage> messages,
		boolean includeStaySearchTool) {

	public BlLlmRequest {
		messages = messages == null ? List.of() : List.copyOf(messages);
	}
}
