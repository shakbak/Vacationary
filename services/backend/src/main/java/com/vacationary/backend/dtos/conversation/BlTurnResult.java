package com.vacationary.backend.dtos.conversation;

import com.vacationary.backend.dtos.assistant.BlAssistantResponse;

public record BlTurnResult(BlAssistantResponse response, boolean duplicate) {
}
