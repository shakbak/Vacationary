package com.vacationary.backend.dtos.http;

import java.util.List;

public record ApiTurnResponse(
		String outcome,
		String text,
		List<ApiStayOption> recommendations,
		boolean duplicate) {

	public ApiTurnResponse {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}
}
