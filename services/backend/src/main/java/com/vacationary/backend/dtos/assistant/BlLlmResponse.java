package com.vacationary.backend.dtos.assistant;

public sealed interface BlLlmResponse {

	record Text(String text) implements BlLlmResponse {
	}

	record ToolRequest(String name, String jsonArgs, String id) implements BlLlmResponse {
	}

	record Error(String message) implements BlLlmResponse {
	}
}
