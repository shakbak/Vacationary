package com.vacationary.backend.core.interfaces.assistant;

import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;

public interface ILlmPromptBuilder {

	String STAY_SEARCH_TOOL = "stay_search";

	BlLlmRequest build(BlAssistantRequest request);

	BlLlmRequest withToolResult(
			BlLlmRequest original,
			BlLlmResponse.ToolRequest toolRequest,
			StaySearchResult.Success success);
}
