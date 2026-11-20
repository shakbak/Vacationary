package com.vacationary.backend.dtos.conversation;

public record BlTurnCommand(BlConversationKey conversation, String text, String eventId) {
}
