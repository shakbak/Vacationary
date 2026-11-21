package com.vacationary.backend.dtos.http;

import java.util.List;

public record ApiMessage(String role, String text, List<ApiStayOption> recommendations) {

	public ApiMessage {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}
}
