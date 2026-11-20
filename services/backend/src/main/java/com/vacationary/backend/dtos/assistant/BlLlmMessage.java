package com.vacationary.backend.dtos.assistant;

public sealed interface BlLlmMessage {

	record User(String content) implements BlLlmMessage {
	}

	record Assistant(String content) implements BlLlmMessage {
	}

	record AssistantToolCall(String id, String name, String jsonArgs) implements BlLlmMessage {
	}

	record Tool(String toolCallId, String content) implements BlLlmMessage {
	}
}
