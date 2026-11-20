package com.vacationary.backend.domain.assistant;

import com.vacationary.backend.core.interfaces.assistant.ILlmClient;
import com.vacationary.backend.core.interfaces.assistant.ILlmManager;
import com.vacationary.backend.core.interfaces.assistant.ILlmPromptBuilder;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;

public class LlmManager implements ILlmManager {

	private final ILlmPromptBuilder promptBuilder;
	private final ILlmClient llmClient;

	public LlmManager(ILlmPromptBuilder promptBuilder, ILlmClient llmClient) {
		this.promptBuilder = promptBuilder;
		this.llmClient = llmClient;
	}

	@Override
	public LlmPromptAndResponse complete(BlAssistantRequest request) {
		BlLlmRequest llmRequest = promptBuilder.build(request);
		BlLlmResponse llmResponse = llmClient.complete(llmRequest);
		return new LlmPromptAndResponse(llmRequest, llmResponse);
	}

	@Override
	public BlLlmResponse completeWithToolResult(
			BlLlmRequest request,
			BlLlmResponse.ToolRequest toolRequest,
			StaySearchResult.Success success) {
		BlLlmRequest followUp = promptBuilder.withToolResult(request, toolRequest, success);
		return llmClient.complete(followUp);
	}
}
