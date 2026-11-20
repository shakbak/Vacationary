package com.vacationary.backend.dtos.conversation;

import java.time.Instant;
import java.util.List;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.ChatRole;

public record DbMessage(
		ChatRole role,
		String text,
		Instant time,
		AssistantResponseKind outcomeKind,
		List<DbStayOption> recommendations) {

	public DbMessage {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}
}
