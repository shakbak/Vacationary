package com.vacationary.backend.persistence.inmemory.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.recommendation.DbStayListing;

class InMemoryStayRepositoryTest {

	private final InMemoryStayRepository repository = InMemoryStayRepository.withFixtures();

	@Test
	void findsFixtureCity() {
		List<DbStayListing> rome = repository.findByDestination("Rome");
		assertTrue(rome.size() >= 5);
		assertTrue(rome.stream().allMatch(listing -> listing.cityName().equals("Rome")));
	}

	@Test
	void unknownCityIsEmpty() {
		assertTrue(repository.findByDestination("Reykjavik").isEmpty());
	}

	@Test
	void normalizeCollapsesCaseAndSpaces() {
		assertEquals(repository.findByDestination("Rome").size(), repository.findByDestination("  rome ").size());
		assertEquals("new york", InMemoryStayRepository.normalize(" New   York "));
	}
}
