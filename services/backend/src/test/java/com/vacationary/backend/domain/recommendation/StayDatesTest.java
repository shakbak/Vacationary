package com.vacationary.backend.domain.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;

class StayDatesTest {

	@Test
	void nightsRequireCheckoutAfterCheckIn() {
		assertEquals(0, StayDates.nights(null));
		assertEquals(0, StayDates.nights(BlStaySearchQuery.ofDestination("Rome")));
		assertEquals(0, StayDates.nights(query(LocalDate.of(2026, 6, 1), null)));
		assertEquals(0, StayDates.nights(query(null, LocalDate.of(2026, 6, 5))));
		assertEquals(0, StayDates.nights(query(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 1))));
		assertEquals(0, StayDates.nights(query(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1))));
		assertEquals(4, StayDates.nights(query(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5))));
		assertEquals(1, StayDates.nights(query(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2))));
	}

	@Test
	void staySummaryAndPriceUseNightsWhenDatesAreUsable() {
		BlStaySearchQuery june = query(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));
		assertEquals("4 nights, 1–5 Jun", StayDates.staySummary(june));
		assertEquals("$175 / night · 4 nights", StayDates.formatPrice(175, june));
		assertNull(StayDates.tripTotal(june, 175));
		assertEquals(
				"About $700 for the stay",
				StayDates.tripTotal(new BlStaySearchQuery(
						"Rome", june.checkIn(), june.checkOut(), 200, null, null, null), 175));
		assertEquals("$175 / night", StayDates.formatPrice(175, BlStaySearchQuery.ofDestination("Rome")));
		assertNull(StayDates.staySummary(query(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 1))));
	}

	@Test
	void formatRangeCoversSameMonthCrossMonthAndYear() {
		assertEquals("1–5 Jun", StayDates.formatRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5)));
		assertEquals("30 May–2 Jun", StayDates.formatRange(LocalDate.of(2026, 5, 30), LocalDate.of(2026, 6, 2)));
		assertEquals(
				"30 Dec 2026–2 Jan 2027",
				StayDates.formatRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)));
	}

	private static BlStaySearchQuery query(LocalDate checkIn, LocalDate checkOut) {
		return new BlStaySearchQuery("Rome", checkIn, checkOut, null, null, null, null);
	}
}
