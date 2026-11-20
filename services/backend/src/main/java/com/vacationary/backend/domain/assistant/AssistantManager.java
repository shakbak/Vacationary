package com.vacationary.backend.domain.assistant;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.assistant.ILlmManager;
import com.vacationary.backend.core.interfaces.assistant.ILlmPromptBuilder;
import com.vacationary.backend.core.interfaces.recommendation.IStayManager;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;
import com.vacationary.backend.errors.bl.BlAssistantException;
import com.vacationary.backend.errors.bl.BlValidationError;

public class AssistantManager implements IAssistantManager {

	private final ILlmManager llmManager;
	private final StaySearchParser staySearchParser;
	private final IStayManager stayManager;
	private final AssistantResponseMapper responseMapper;

	public AssistantManager(
			ILlmManager llmManager,
			StaySearchParser staySearchParser,
			IStayManager stayManager,
			AssistantResponseMapper responseMapper) {
		this.llmManager = llmManager;
		this.staySearchParser = staySearchParser;
		this.stayManager = stayManager;
		this.responseMapper = responseMapper;
	}

	@Override
	public BlAssistantResponse respond(BlAssistantRequest request) {
		if (request == null) {
			throw new BlAssistantException("request is required");
		}
		try {
			ILlmManager.LlmPromptAndResponse promptAndResponse = llmManager.complete(request);
			return switch (promptAndResponse.response()) {
				case BlLlmResponse.Error ignored -> responseMapper.failProcess();
				case BlLlmResponse.Text text -> responseMapper.fromModelText(text.text());
				case BlLlmResponse.ToolRequest toolRequest -> runStaySearchThenReply(promptAndResponse.request(), toolRequest);
			};
		}
		catch (BlValidationError | BlAssistantException error) {
			throw error;
		}
		catch (RuntimeException error) {
			throw new BlAssistantException("Assistant failed", error);
		}
	}

	private BlAssistantResponse runStaySearchThenReply(
			BlLlmRequest llmRequest,
			BlLlmResponse.ToolRequest toolRequest) {
		if (!ILlmPromptBuilder.STAY_SEARCH_TOOL.equals(toolRequest.name())) {
			return responseMapper.failProcess();
		}
		StaySearchParser.Result parsed = staySearchParser.parse(toolRequest.jsonArgs());
		if (parsed instanceof StaySearchParser.Result.MissingDestination) {
			return responseMapper.clarifyDestination();
		}
		if (!(parsed instanceof StaySearchParser.Result.ParsedQuery parsedQuery)) {
			return responseMapper.failSearch();
		}
		StaySearchResult result = stayManager.search(parsedQuery.query());
		if (!(result instanceof StaySearchResult.Success success)) {
			return responseMapper.failSearch();
		}
		BlLlmResponse reply = llmManager.completeWithToolResult(llmRequest, toolRequest, success);
		return responseMapper.recommend(reply, success.options());
	}
}
