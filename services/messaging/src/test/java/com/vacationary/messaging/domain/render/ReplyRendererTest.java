package com.vacationary.messaging.domain.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.messaging.dtos.BlStayOption;
import com.vacationary.messaging.dtos.BlTurnResult;

class ReplyRendererTest {

	private final ReplyRenderer renderer = new ReplyRenderer();

	@Test
	void recommendListsFiveStaysWithTypeWordsAndUrl() {
		String body = renderer.render(new BlTurnResult(
				"recommend",
				"""
						Here are five lodging options in Rome.

						| Name | Why it stands out |
						|------|-------------------|
						| Alpha Inn | Quiet courtyard |
						""",
				List.of(
						stay("Alpha Inn", "hotel"),
						stay("Bravo House", "vacation_rental"),
						stay("Charlie B&B", "bnb"),
						stay("Delta Hostel", "hostel"),
						stay("Echo Motel", "motel")),
				false));
		assertTrue(body.startsWith("Here are five lodging options in Rome."));
		assertFalse(body.contains("Why it stands out"));
		assertTrue(body.contains("*Alpha Inn*"));
		assertTrue(body.contains("Hotel · Trastevere, Rome"));
		assertTrue(body.contains("Vacation rental · Trastevere, Rome"));
		assertTrue(body.contains("B&B · Trastevere, Rome"));
		assertTrue(body.contains("$120 / night · Rating 4.6"));
		assertTrue(body.contains("Quiet courtyard"));
		assertTrue(body.contains("https://example.com/Alpha Inn"));
	}

	@Test
	void clarifyIsPlainText() {
		String body = renderer.render(new BlTurnResult(
				"clarify", "Where would you like to stay?", List.of(), false));
		assertEquals("Where would you like to stay?", body);
	}

	private static BlStayOption stay(String name, String type) {
		return new BlStayOption(
				name,
				type,
				"Trastevere, Rome",
				"$120 / night",
				"4.6",
				"Quiet courtyard",
				"https://example.com/" + name);
	}
}
