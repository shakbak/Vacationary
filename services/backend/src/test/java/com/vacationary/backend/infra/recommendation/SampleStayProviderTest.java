package com.vacationary.backend.infra.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.StayType;

class SampleStayProviderTest {

	private final SampleStayProvider provider = new SampleStayProvider();

	@Test
	void generatesFiveDistinctOptionsForAnyCity() {
		List<BlStayOption> options = provider.generate(BlStaySearchQuery.ofDestination("cape town"));
		assertEquals(5, options.size());
		assertEquals(5, options.stream().map(BlStayOption::name).collect(Collectors.toSet()).size());
		assertTrue(options.stream().allMatch(option -> option.location().contains("Cape Town")));
		assertTrue(options.stream().allMatch(option -> option.url().startsWith("https://")));
		assertTrue(options.stream().noneMatch(option -> option.name().matches("Hotel \\d+")));
	}

	@Test
	void respectsRequestedStayType() {
		List<BlStayOption> options = provider.generate(new BlStaySearchQuery(
				"Oslo", null, null, null, null, StayType.HOSTEL, null));
		assertTrue(options.stream().allMatch(option -> option.stayType() == StayType.HOSTEL));
	}

	@Test
	void sameCityIsDeterministic() {
		BlStaySearchQuery query = BlStaySearchQuery.ofDestination("Kyoto");
		assertEquals(names(provider.generate(query)), names(provider.generate(query)));
	}

	private static Set<String> names(List<BlStayOption> options) {
		return options.stream().map(BlStayOption::name).collect(Collectors.toSet());
	}
}
