package com.vacationary.messaging.domain.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StayTypeLabelTest {

	@Test
	void mapsEnumsToOrdinaryWords() {
		assertEquals("Vacation rental", StayTypeLabel.of("VACATION_RENTAL"));
		assertEquals("B&B", StayTypeLabel.of("bnb"));
		assertEquals("Hotel", StayTypeLabel.of("hotel"));
	}
}
