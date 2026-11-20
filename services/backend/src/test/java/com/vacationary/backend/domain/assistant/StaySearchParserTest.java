package com.vacationary.backend.domain.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.StayType;

class StaySearchParserTest {

	private final StaySearchParser parser = new StaySearchParser(new ObjectMapper());

	@Test
	void missingDestination() {
		assertInstanceOf(StaySearchParser.Result.MissingDestination.class, parser.parse("{}"));
		assertInstanceOf(StaySearchParser.Result.MissingDestination.class, parser.parse("{\"destination\":\"  \"}"));
		assertInstanceOf(StaySearchParser.Result.MissingDestination.class, parser.parse(""));
	}

	@Test
	void parseReadsOptionalFiltersFromArgsOnly() {
		StaySearchParser.Result.ParsedQuery parsed = assertInstanceOf(
				StaySearchParser.Result.ParsedQuery.class,
				parser.parse("""
						{"destination":"Lisbon","checkIn":"2026-06-01","checkOut":"2026-06-05",
						 "budgetMax":150,"guestCount":4,"stayType":"hostel","preferences":"quiet"}
						"""));
		BlStaySearchQuery query = parsed.query();
		assertEquals("Lisbon", query.destination());
		assertEquals(LocalDate.of(2026, 6, 1), query.checkIn());
		assertEquals(LocalDate.of(2026, 6, 5), query.checkOut());
		assertEquals(150, query.budgetMax());
		assertEquals(4, query.guestCount());
		assertEquals(StayType.HOSTEL, query.stayType());
		assertEquals("quiet", query.preferences());
	}

	@Test
	void invalidJsonIsInvalidArguments() {
		assertInstanceOf(StaySearchParser.Result.InvalidArguments.class, parser.parse("{"));
	}
}
