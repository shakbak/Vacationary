package com.vacationary.backend.dtos.recommendation;

public record DbStayListing(
		String cityKey,
		String cityName,
		String name,
		StayType stayType,
		String area,
		int pricePerNight,
		double rating,
		String url,
		int sleeps,
		String amenities) {
}
