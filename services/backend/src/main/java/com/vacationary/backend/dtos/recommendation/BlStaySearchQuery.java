package com.vacationary.backend.dtos.recommendation;

import java.time.LocalDate;

public record BlStaySearchQuery(
		String destination,
		LocalDate checkIn,
		LocalDate checkOut,
		Integer budgetMax,
		Integer guestCount,
		StayType stayType,
		String preferences) {

	public static BlStaySearchQuery ofDestination(String destination) {
		return new BlStaySearchQuery(destination, null, null, null, null, null, null);
	}
}
