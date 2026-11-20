package com.vacationary.backend.persistence.jdbc.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.recommendation.DbStayListing;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;

class JdbcStayRepositoryTest {

	private final JdbcStayRepository repository = new JdbcStayRepository(H2TestDataSource.create());

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
	void blankDestinationIsEmpty() {
		assertTrue(repository.findByDestination(null).isEmpty());
		assertTrue(repository.findByDestination("  ").isEmpty());
	}

	@Test
	void normalizeCollapsesCaseAndSpaces() {
		assertEquals(repository.findByDestination("Rome").size(), repository.findByDestination("  rome ").size());
		assertEquals("new york", JdbcStayRepository.normalize(" New   York "));
	}
}
