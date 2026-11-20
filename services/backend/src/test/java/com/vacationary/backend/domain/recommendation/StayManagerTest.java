package com.vacationary.backend.domain.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.core.interfaces.recommendation.IStayProvider;
import com.vacationary.backend.core.interfaces.recommendation.IStayRepository;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.DbStayListing;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.infra.recommendation.SampleStayProvider;
import com.vacationary.backend.persistence.inmemory.recommendation.InMemoryStayRepository;

class StayManagerTest {

	private final StayManager manager = new StayManager(
			InMemoryStayRepository.withFixtures(),
			new SampleStayProvider(),
			new StayListingMapper());

	@Test
	void blankDestinationThrowsValidationError() {
		assertThrows(BlValidationError.class, () -> manager.search(BlStaySearchQuery.ofDestination("  ")));
		assertThrows(BlValidationError.class, () -> manager.search(BlStaySearchQuery.ofDestination(null)));
		assertThrows(BlValidationError.class, () -> manager.search(null));
	}

	@Test
	void knownCityReturnsFiveNamedOptions() {
		StaySearchResult.Success success = searchRome(BlStaySearchQuery.ofDestination("Rome"));
		assertEquals(5, success.options().size());
		assertEquals(5, names(success).size());
		assertTrue(success.options().stream().allMatch(option -> option.location().contains("Rome")));
		assertTrue(success.options().stream().allMatch(option -> option.url().startsWith("https://")));
	}

	@Test
	void destinationMatchIsCaseInsensitiveAndTrimmed() {
		StaySearchResult.Success upper = searchRome(BlStaySearchQuery.ofDestination("  ROME  "));
		StaySearchResult.Success lower = searchRome(BlStaySearchQuery.ofDestination("rome"));
		assertEquals(names(lower), names(upper));
	}

	@Test
	void unknownCityUsesProviderAndIsNotFailure() {
		StaySearchResult.Success success = requireSuccess(manager.search(BlStaySearchQuery.ofDestination("Reykjavik")));
		assertEquals(5, success.options().size());
		assertEquals(5, names(success).size());
		assertTrue(success.options().stream().allMatch(option -> option.location().contains("Reykjavik")));
	}

	@Test
	void stayTypeIsPreferredThenPaddedFromRepository() {
		StaySearchResult.Success success = searchRome(new BlStaySearchQuery(
				"Rome", null, null, null, null, StayType.HOTEL, null));
		assertEquals(StayType.HOTEL, success.options().get(0).stayType());
		assertEquals(StayType.HOTEL, success.options().get(1).stayType());
		assertTrue(success.options().get(2).why().contains("Not a full match"));
	}

	@Test
	void budgetPrefersListingsAtOrUnderMax() {
		StaySearchResult.Success success = searchRome(new BlStaySearchQuery(
				"Rome", null, null, 100, null, null, null));
		assertTrue(success.options().get(0).price().contains("$45")
				|| success.options().get(0).price().contains("$38")
				|| success.options().get(0).price().contains("$85")
				|| success.options().get(0).price().contains("$95"));
		assertTrue(success.options().stream().anyMatch(option -> option.why().contains("$") && option.why().contains("per night")));
	}

	@Test
	void guestCountPrefersListingsThatSleepEnoughPeople() {
		StaySearchResult.Success success = searchRome(new BlStaySearchQuery(
				"Rome", null, null, null, 5, null, null));
		assertTrue(success.options().get(0).why().contains("Sleeps"));
		assertTrue(success.options().get(0).name().contains("Hostel")
				|| success.options().get(0).name().contains("Loft")
				|| success.options().get(0).name().contains("Family"));
	}

	@Test
	void datesAreAcceptedAndIgnoredForRanking() {
		StaySearchResult.Success withDates = searchRome(new BlStaySearchQuery(
				"Rome",
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 5),
				null,
				null,
				null,
				null));
		StaySearchResult.Success withoutDates = searchRome(BlStaySearchQuery.ofDestination("Rome"));
		assertEquals(names(withoutDates), names(withDates));
	}

	@Test
	void thinRepositoryIsPaddedByProvider() {
		IStayRepository thin = destination -> List.of(listing("Oslo", "Oslo Dock Hotel", StayType.HOTEL, 2));
		StayManager thinManager = new StayManager(thin, new SampleStayProvider(), new StayListingMapper());
		StaySearchResult.Success success = requireSuccess(thinManager.search(BlStaySearchQuery.ofDestination("Oslo")));
		assertEquals(5, success.options().size());
		assertEquals("Oslo Dock Hotel", success.options().get(0).name());
		assertEquals(5, names(success).size());
	}

	@Test
	void repositoryExceptionBecomesFailure() {
		IStayRepository broken = destination -> {
			throw new IllegalStateException("repository down");
		};
		IStayProvider unused = query -> List.of();
		StayManager failing = new StayManager(broken, unused, new StayListingMapper());
		StaySearchResult result = failing.search(BlStaySearchQuery.ofDestination("Rome"));
		StaySearchResult.Failure failure = assertInstanceOf(StaySearchResult.Failure.class, result);
		assertEquals("Stay search failed", failure.message());
	}

	private StaySearchResult.Success searchRome(BlStaySearchQuery query) {
		return requireSuccess(manager.search(query));
	}

	private static StaySearchResult.Success requireSuccess(StaySearchResult result) {
		return assertInstanceOf(StaySearchResult.Success.class, result);
	}

	private static Set<String> names(StaySearchResult.Success success) {
		return success.options().stream().map(BlStayOption::name).collect(Collectors.toSet());
	}

	private static DbStayListing listing(String city, String name, StayType type, int sleeps) {
		return new DbStayListing(
				city.toLowerCase(),
				city,
				name,
				type,
				"Center",
				120,
				4.2,
				"https://www.booking.com/hotel/sample/oslo-dock.html",
				sleeps,
				"quiet");
	}
}
