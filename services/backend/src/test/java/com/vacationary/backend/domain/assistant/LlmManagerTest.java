package com.vacationary.backend.domain.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.core.interfaces.assistant.ILlmManager;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.BlLlmMessage;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.infra.llm.FakeLlmClient;

class LlmManagerTest {

	private final LlmPromptBuilder promptBuilder = new LlmPromptBuilder(new ObjectMapper());

	@Test
	void completeBuildsThePromptAndReturnsTheModelResponse() {
		FakeLlmClient llm = new FakeLlmClient(new BlLlmResponse.Text("CLARIFY: Where would you like to stay?"));
		LlmManager manager = new LlmManager(promptBuilder, llm);
		ILlmManager.LlmPromptAndResponse promptAndResponse = manager.complete(userTurn("I need a place to stay"));
		assertTrue(promptAndResponse.request().includeStaySearchTool());
		assertEquals("I need a place to stay", ((BlLlmMessage.User) promptAndResponse.request().messages().getFirst()).content());
		assertInstanceOf(BlLlmResponse.Text.class, promptAndResponse.response());
		assertEquals(1, llm.capturedRequests().size());
		assertEquals(promptAndResponse.request(), llm.capturedRequests().getFirst());
	}

	@Test
	void completeWithToolResultDisablesASecondSearch() {
		FakeLlmClient llm = new FakeLlmClient(
				new BlLlmResponse.ToolRequest("stay_search", "{\"destination\":\"Rome\"}", "c1"),
				new BlLlmResponse.Text("Here are five hotels in Rome."));
		LlmManager manager = new LlmManager(promptBuilder, llm);
		ILlmManager.LlmPromptAndResponse first = manager.complete(userTurn("Rome"));
		BlLlmResponse.ToolRequest toolRequest = (BlLlmResponse.ToolRequest) first.response();
		StaySearchResult.Success success = new StaySearchResult.Success(List.of(
				option("One"), option("Two"), option("Three"), option("Four"), option("Five")));
		BlLlmResponse reply = manager.completeWithToolResult(first.request(), toolRequest, success);
		assertInstanceOf(BlLlmResponse.Text.class, reply);
		assertEquals(2, llm.capturedRequests().size());
		assertFalse(llm.capturedRequests().get(1).includeStaySearchTool());
	}

	private static BlAssistantRequest userTurn(String text) {
		return new BlAssistantRequest(List.of(new BlChatMessage(ChatRole.USER, text)), text);
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
