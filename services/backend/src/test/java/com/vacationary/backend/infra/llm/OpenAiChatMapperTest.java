package com.vacationary.backend.infra.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.dtos.assistant.BlLlmMessage;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;

class OpenAiChatMapperTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OpenAiChatMapper mapper = new OpenAiChatMapper(objectMapper);

	@Test
	void requestIncludesSystemUserAndStaySearchTool() {
		BlLlmRequest request = new BlLlmRequest(
				"system text",
				List.of(new BlLlmMessage.User("Rome")),
				true);
		JsonNode body = objectMapper.readTree(mapper.toRequestJson(request, "gpt-4o-mini"));
		assertEquals("gpt-4o-mini", body.get("model").asText());
		assertEquals("system", body.get("messages").get(0).get("role").asText());
		assertEquals("Rome", body.get("messages").get(1).get("content").asText());
		assertEquals("stay_search", body.get("tools").get(0).get("function").get("name").asText());
	}

	@Test
	void secondTurnOmitsTools() {
		BlLlmRequest request = new BlLlmRequest("system", List.of(new BlLlmMessage.User("Rome")), false);
		JsonNode body = objectMapper.readTree(mapper.toRequestJson(request, "gpt-4o-mini"));
		assertFalse(body.has("tools"));
	}

	@Test
	void parsesToolRequestAndTextAndError() {
		BlLlmResponse.ToolRequest tool = assertInstanceOf(BlLlmResponse.ToolRequest.class, mapper.toResponse("""
				{"choices":[{"message":{"tool_calls":[{"id":"c1","function":{"name":"stay_search","arguments":"{\\"destination\\":\\"Rome\\"}"}}]}}]}
				"""));
		assertEquals("stay_search", tool.name());
		assertTrue(tool.jsonArgs().contains("Rome"));

		BlLlmResponse.Text text = assertInstanceOf(BlLlmResponse.Text.class, mapper.toResponse("""
				{"choices":[{"message":{"content":"CLARIFY: Where?"}}]}
				"""));
		assertEquals("CLARIFY: Where?", text.text());

		BlLlmResponse.Error error = assertInstanceOf(BlLlmResponse.Error.class, mapper.toResponse("""
				{"error":{"message":"quota"}}
				"""));
		assertEquals("quota", error.message());
	}
}
