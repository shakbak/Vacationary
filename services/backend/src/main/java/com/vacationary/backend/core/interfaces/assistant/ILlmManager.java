package com.vacationary.backend.core.interfaces.assistant;

import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;

public interface ILlmManager {

	record LlmPromptAndResponse(BlLlmRequest request, BlLlmResponse response) {
	}

	LlmPromptAndResponse complete(BlAssistantRequest request);

	BlLlmResponse completeWithToolResult(
			BlLlmRequest request,
			BlLlmResponse.ToolRequest toolRequest,
			StaySearchResult.Success success);
}
