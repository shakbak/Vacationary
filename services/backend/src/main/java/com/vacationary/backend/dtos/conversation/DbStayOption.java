package com.vacationary.backend.dtos.conversation;

import com.vacationary.backend.dtos.recommendation.StayType;

public record DbStayOption(
		String name,
		StayType stayType,
		String location,
		String price,
		String rating,
		String why,
		String url) {
}
