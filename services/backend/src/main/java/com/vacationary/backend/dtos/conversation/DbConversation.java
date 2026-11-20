package com.vacationary.backend.dtos.conversation;

import java.util.List;

public record DbConversation(
		ConversationChannel channel,
		String externalUserId,
		List<DbMessage> messages) {

	public DbConversation {
		messages = messages == null ? List.of() : List.copyOf(messages);
	}
}
