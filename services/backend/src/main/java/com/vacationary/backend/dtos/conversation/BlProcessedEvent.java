package com.vacationary.backend.dtos.conversation;

import com.vacationary.backend.dtos.assistant.BlAssistantResponse;

public record BlProcessedEvent(BlEventKey key, BlAssistantResponse outcome) {
}
