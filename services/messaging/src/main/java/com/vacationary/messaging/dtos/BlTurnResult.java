package com.vacationary.messaging.dtos;

import java.util.List;

public record BlTurnResult(
		String outcome,
		String text,
		List<BlStayOption> recommendations,
		boolean duplicate) {

	public BlTurnResult {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}

	public static BlTurnResult fail(String text) {
		return new BlTurnResult("fail", text, List.of(), false);
	}
}
