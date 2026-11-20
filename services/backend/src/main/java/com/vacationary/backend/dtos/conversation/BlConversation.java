package com.vacationary.backend.dtos.conversation;

import java.util.ArrayList;
import java.util.List;

public record BlConversation(BlConversationKey key, List<BlMessage> messages) {

	public BlConversation {
		messages = messages == null ? List.of() : List.copyOf(messages);
	}

	public static BlConversation empty(BlConversationKey key) {
		return new BlConversation(key, List.of());
	}

	public BlConversation withMessage(BlMessage message) {
		List<BlMessage> next = new ArrayList<>(messages);
		next.add(message);
		return new BlConversation(key, next);
	}
}
