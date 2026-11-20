package com.vacationary.backend.dtos.recommendation;

import java.util.List;

public sealed interface StaySearchResult {

	record Success(List<BlStayOption> options) implements StaySearchResult {
		public Success {
			options = List.copyOf(options);
		}
	}

	record Failure(String message) implements StaySearchResult {
	}
}
