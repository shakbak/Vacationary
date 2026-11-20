package com.vacationary.backend.dtos.assistant;

import java.util.List;

import com.vacationary.backend.dtos.recommendation.BlStayOption;

public record BlAssistantResponse(
		AssistantResponseKind kind,
		String text,
		List<BlStayOption> recommendations) {

	public BlAssistantResponse {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}

	public static BlAssistantResponse clarify(String text) {
		return new BlAssistantResponse(AssistantResponseKind.CLARIFY, text, List.of());
	}

	public static BlAssistantResponse refuseScope(String text) {
		return new BlAssistantResponse(AssistantResponseKind.REFUSE_SCOPE, text, List.of());
	}

	public static BlAssistantResponse fail(String text) {
		return new BlAssistantResponse(AssistantResponseKind.FAIL, text, List.of());
	}

	public static BlAssistantResponse recommend(String text, List<BlStayOption> options) {
		return new BlAssistantResponse(AssistantResponseKind.RECOMMEND, text, options);
	}
}
