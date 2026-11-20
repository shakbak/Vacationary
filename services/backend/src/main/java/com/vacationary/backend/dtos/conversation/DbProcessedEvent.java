package com.vacationary.backend.dtos.conversation;

import java.util.List;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;

public record DbProcessedEvent(
		ConversationChannel channel,
		String eventId,
		AssistantResponseKind kind,
		String text,
		List<DbStayOption> recommendations) {

	public DbProcessedEvent {
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
	}
}
