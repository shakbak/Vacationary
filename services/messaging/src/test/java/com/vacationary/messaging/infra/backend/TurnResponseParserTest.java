package com.vacationary.messaging.infra.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.dtos.BlTurnResult;

class TurnResponseParserTest {

	private final TurnResponseParser parser = new TurnResponseParser(new ObjectMapper());

	@Test
	void mapsRecommendJson() {
		BlTurnResult result = parser.parse("""
				{"outcome":"recommend","text":"Here you go.","recommendations":[{
				  "name":"Alpha Inn","stayType":"hotel","location":"Rome",
				  "price":"$100 / night","rating":"4.5","why":"Quiet","url":"https://example.com/a"
				}],"duplicate":false}
				""");
		assertEquals("recommend", result.outcome());
		assertEquals("Here you go.", result.text());
		assertFalse(result.duplicate());
		assertEquals("Alpha Inn", result.recommendations().get(0).name());
		assertEquals("Quiet", result.recommendations().get(0).why());
	}

	@Test
	void blankOrMissingOutcomeIsFail() {
		assertEquals(ReplyCopy.BACKEND_UNREACHABLE, parser.parse("").text());
		assertEquals("fail", parser.parse("{}").outcome());
		assertEquals("fail", parser.parse("not-json").outcome());
		assertTrue(parser.parse(null).recommendations().isEmpty());
	}
}
