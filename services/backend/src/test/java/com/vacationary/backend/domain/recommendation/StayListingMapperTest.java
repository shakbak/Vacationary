package com.vacationary.backend.domain.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.DbStayListing;
import com.vacationary.backend.dtos.recommendation.StayType;

class StayListingMapperTest {

	private final StayListingMapper mapper = new StayListingMapper();

	@Test
	void mapsDisplayFieldsAndWhyFromQuery() {
		DbStayListing listing = new DbStayListing(
				"rome",
				"Rome",
				"Pantheon Loft Rental",
				StayType.VACATION_RENTAL,
				"Pantheon",
				175,
				4.5,
				"https://www.booking.com/hotel/sample/pantheon-loft.html",
				5,
				"family kitchen");
		BlStaySearchQuery query = new BlStaySearchQuery(
				"Rome", null, null, 200, 4, StayType.VACATION_RENTAL, null);
		BlStayOption option = mapper.toBl(listing, query, true);
		assertEquals("Pantheon Loft Rental", option.name());
		assertEquals("Pantheon, Rome", option.location());
		assertEquals("$175 / night", option.price());
		assertEquals("4.5", option.rating());
		assertTrue(option.why().contains("Sleeps 5"));
		assertTrue(option.why().contains("Requested stay type"));
		assertFalse(option.why().contains("Not a full match"));
	}

	@Test
	void datesAppearInWhyAndPriceWithoutChangingMatchCopy() {
		DbStayListing listing = new DbStayListing(
				"rome",
				"Rome",
				"Pantheon Loft Rental",
				StayType.VACATION_RENTAL,
				"Pantheon",
				175,
				4.5,
				"https://www.booking.com/hotel/sample/pantheon-loft.html",
				5,
				"family kitchen");
		BlStaySearchQuery query = new BlStaySearchQuery(
				"Rome",
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 5),
				200,
				4,
				StayType.VACATION_RENTAL,
				null);
		BlStayOption option = mapper.toBl(listing, query, true);
		assertEquals("$175 / night · 4 nights", option.price());
		assertTrue(option.why().contains("4 nights, 1–5 Jun"));
		assertTrue(option.why().contains("About $700 for the stay"));
		assertTrue(option.why().contains("$175 per night"));
	}

	@Test
	void notesWhenListingIsNotAFullMatch() {
		DbStayListing listing = new DbStayListing(
				"rome",
				"Rome",
				"Ostia Beach Motel",
				StayType.MOTEL,
				"Ostia",
				85,
				3.9,
				"https://www.booking.com/hotel/sample/ostia-motel.html",
				4,
				"beach");
		BlStayOption option = mapper.toBl(listing, BlStaySearchQuery.ofDestination("Rome"), false);
		assertTrue(option.why().contains("Not a full match for this request"));
	}
}
