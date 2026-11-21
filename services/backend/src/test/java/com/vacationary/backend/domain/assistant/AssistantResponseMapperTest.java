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

	@Test
	void recommendUsesWhyItStandsOutFromTheReplyTable() {
		List<BlStayOption> options = List.of(option("A"), option("B"), option("C"), option("D"), option("E"));
		String text = """
				Here are five lodging options for your trip.

				| Stay | Why it stands out |
				|------|-------------------|
				| A | Quiet courtyard near the Pantheon |
				| B | Easy walk to Termini |
				""";
		BlAssistantResponse response = mapper.recommend(new BlLlmResponse.Text(text), options);
		assertEquals("Quiet courtyard near the Pantheon", response.recommendations().get(0).why());
		assertEquals("Easy walk to Termini", response.recommendations().get(1).why());
		assertEquals("why", response.recommendations().get(2).why());
	}

	@Test
	void recommendReadsWhyFromANumberedDumpWhenThereIsNoTable() {
		List<BlStayOption> options = List.of(option("Hotel Campo de' Fiori"), option("B"), option("C"), option("D"), option("E"));
		String text = """
				Here are five lodging options in Rome:

				1. **Hotel Campo de' Fiori** – HOTEL – Centro Storico, Rome – $210 / night – Rating 4.6 – *Why:* Quiet courtyard a short walk from the market. – [Booking.com](https://example.com)
				""";
		BlAssistantResponse response = mapper.recommend(new BlLlmResponse.Text(text), options);
		assertEquals("Quiet courtyard a short walk from the market.", response.recommendations().get(0).why());
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
