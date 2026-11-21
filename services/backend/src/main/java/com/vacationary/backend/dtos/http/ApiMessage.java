package com.vacationary.backend.dtos.http;

import java.util.List;

public record ApiMessage(String role, String text, List<ApiStayOption> recommendations, String outcome) {

	public ApiMessage {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}
}
