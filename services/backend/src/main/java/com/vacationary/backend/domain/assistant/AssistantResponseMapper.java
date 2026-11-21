package com.vacationary.backend.domain.assistant;

import java.util.ArrayList;
import java.util.List;

import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.recommendation.BlStayOption;

public class AssistantResponseMapper {

	static final String PROCESS_FAIL = "I could not process that message. Please retry.";
	static final String SEARCH_FAIL = "Search did not work. Please retry or rephrase.";
	static final String DESTINATION_QUESTION = "Where would you like to stay?";
	static final String FALLBACK_RECOMMEND = "Here are five lodging options for your trip.";

	public BlAssistantResponse fromModelText(String text) {
		if (text == null || text.isBlank()) {
			return failProcess();
		}
		String trimmed = text.trim();
		if (startsWithIgnoreCase(trimmed, "REFUSE_SCOPE:")) {
			return BlAssistantResponse.refuseScope(stripPrefix(trimmed, "REFUSE_SCOPE:"));
		}
		if (startsWithIgnoreCase(trimmed, "CLARIFY:")) {
			return BlAssistantResponse.clarify(stripPrefix(trimmed, "CLARIFY:"));
		}
		return BlAssistantResponse.clarify(trimmed);
	}

	public BlAssistantResponse failProcess() {
		return BlAssistantResponse.fail(PROCESS_FAIL);
	}

	public BlAssistantResponse failSearch() {
		return BlAssistantResponse.fail(SEARCH_FAIL);
	}

	public BlAssistantResponse clarifyDestination() {
		return BlAssistantResponse.clarify(DESTINATION_QUESTION);
	}

	public BlAssistantResponse recommend(BlLlmResponse llmResponse, List<BlStayOption> options) {
		String text = replyText(llmResponse);
		List<BlStayOption> copied = new ArrayList<>();
		for (BlStayOption option : options) {
			String why = StayWhyParser.whyForStay(text, option.name());
			copied.add(new BlStayOption(
					option.name(),
					option.stayType(),
					option.location(),
					option.price(),
					option.rating(),
					why != null ? why : option.why(),
					option.url()));
		}
		return BlAssistantResponse.recommend(text, copied);
	}

	private static String replyText(BlLlmResponse llmResponse) {
		return switch (llmResponse) {
			case BlLlmResponse.Text text when text.text() != null && !text.text().isBlank() -> text.text().trim();
			default -> FALLBACK_RECOMMEND;
		};
	}

	private static boolean startsWithIgnoreCase(String text, String prefix) {
		return text.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static String stripPrefix(String text, String prefix) {
		String rest = text.substring(prefix.length()).trim();
		return rest.isEmpty() ? text : rest;
	}
}
