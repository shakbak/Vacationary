package com.vacationary.backend.domain.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.BlLlmMessage;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;
import com.vacationary.backend.dtos.recommendation.StayType;

class LlmPromptBuilderTest {

	private final LlmPromptBuilder builder = new LlmPromptBuilder(new ObjectMapper());

	@Test
	void usesConversationMessagesAndEnablesTheTool() {
		BlLlmRequest request = builder.build(new BlAssistantRequest(List.of(
				new BlChatMessage(ChatRole.USER, "hotels in Lisbon"),
				new BlChatMessage(ChatRole.ASSISTANT, "Here are five options."),
				new BlChatMessage(ChatRole.USER, "cheaper")), "cheaper"));
		assertTrue(request.includeStaySearchTool());
		assertTrue(request.systemPrompt().contains("earlier messages"));
		assertTrue(request.systemPrompt().contains("Why it stands out"));
		assertEquals(3, request.messages().size());
		assertInstanceOf(BlLlmMessage.User.class, request.messages().get(0));
		assertInstanceOf(BlLlmMessage.Assistant.class, request.messages().get(1));
		assertEquals("cheaper", ((BlLlmMessage.User) request.messages().get(2)).content());
	}

	@Test
	void fallsBackToLatestUserTextWhenMessagesAreEmpty() {
		BlLlmRequest request = builder.build(new BlAssistantRequest(List.of(), "Rome please"));
		assertEquals(1, request.messages().size());
		assertEquals("Rome please", ((BlLlmMessage.User) request.messages().getFirst()).content());
	}

	@Test
	void toolResultDisablesASecondSearch() {
		BlLlmRequest first = builder.build(new BlAssistantRequest(
				List.of(new BlChatMessage(ChatRole.USER, "Rome")), "Rome"));
		StaySearchResult.Success success = new StaySearchResult.Success(List.of(
				option("One"), option("Two"), option("Three"), option("Four"), option("Five")));
		BlLlmRequest second = builder.withToolResult(
				first,
				new BlLlmResponse.ToolRequest("stay_search", "{\"destination\":\"Rome\"}", "call-1"),
				success);
		assertFalse(second.includeStaySearchTool());
		assertInstanceOf(BlLlmMessage.AssistantToolCall.class, second.messages().get(1));
		BlLlmMessage.Tool tool = assertInstanceOf(BlLlmMessage.Tool.class, second.messages().get(2));
		assertTrue(tool.content().contains("One"));
		assertTrue(tool.content().contains("\"highlights\""));
		assertFalse(tool.content().contains("\"why\":"));
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
