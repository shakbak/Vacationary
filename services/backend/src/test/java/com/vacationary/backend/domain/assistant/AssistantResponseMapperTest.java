package com.vacationary.backend.domain.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;

class AssistantResponseMapperTest {

	private final AssistantResponseMapper mapper = new AssistantResponseMapper();

	@Test
	void prefixedClarifyAndRefuseHaveNoRecommendations() {
		BlAssistantResponse clarify = mapper.fromModelText("CLARIFY: Which city?");
		assertEquals(AssistantResponseKind.CLARIFY, clarify.kind());
		assertEquals("Which city?", clarify.text());
		assertTrue(clarify.recommendations().isEmpty());

		BlAssistantResponse refuse = mapper.fromModelText("REFUSE_SCOPE: I only help with lodging.");
		assertEquals(AssistantResponseKind.REFUSE_SCOPE, refuse.kind());
		assertTrue(refuse.recommendations().isEmpty());
	}

	@Test
	void emptyModelTextIsProcessFailure() {
		BlAssistantResponse response = mapper.fromModelText("  ");
		assertEquals(AssistantResponseKind.FAIL, response.kind());
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void recommendCopiesToolOptionsAndUsesFallbackWhenReplyIsBlankOrNotText() {
		List<BlStayOption> options = List.of(option("A"), option("B"), option("C"), option("D"), option("E"));
		BlAssistantResponse blank = mapper.recommend(new BlLlmResponse.Text("  "), options);
		assertEquals(AssistantResponseKind.RECOMMEND, blank.kind());
		assertEquals(AssistantResponseMapper.FALLBACK_RECOMMEND, blank.text());
		assertEquals("A", blank.recommendations().get(0).name());
		assertEquals(5, blank.recommendations().size());

		BlAssistantResponse error = mapper.recommend(new BlLlmResponse.Error("write failed"), options);
		assertEquals(AssistantResponseKind.RECOMMEND, error.kind());
		assertEquals(AssistantResponseMapper.FALLBACK_RECOMMEND, error.text());
		assertEquals(5, error.recommendations().size());
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
