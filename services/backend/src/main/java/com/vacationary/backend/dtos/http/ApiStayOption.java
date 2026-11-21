package com.vacationary.backend.dtos.http;

import java.util.Locale;

import com.vacationary.backend.dtos.recommendation.BlStayOption;

public record ApiStayOption(
		String name,
		String stayType,
		String location,
		String price,
		String rating,
		String why,
		String url) {

	public static ApiStayOption fromBl(BlStayOption option) {
		return new ApiStayOption(
				option.name(),
				option.stayType() == null ? null : option.stayType().name().toLowerCase(Locale.ROOT),
				option.location(),
				option.price(),
				option.rating(),
				option.why(),
				option.url());
	}
}
