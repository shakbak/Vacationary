package com.vacationary.backend.core.interfaces.assistant;

import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;

public interface IOpenAiChatMapper {

	String toRequestJson(BlLlmRequest request, String model);

	BlLlmResponse toResponse(String rawJson);
}
