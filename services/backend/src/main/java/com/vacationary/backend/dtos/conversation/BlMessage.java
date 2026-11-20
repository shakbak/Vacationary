package com.vacationary.backend.dtos.conversation;

import java.time.Instant;
import java.util.List;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.recommendation.BlStayOption;

public record BlMessage(
		ChatRole role,
		String text,
		Instant time,
		AssistantResponseKind outcomeKind,
		List<BlStayOption> recommendations) {

	public BlMessage {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}
}
