package com.vacationary.backend.dtos.recommendation;

public record BlStayOption(
		String name,
		StayType stayType,
		String location,
		String price,
		String rating,
		String why,
		String url) {
}
