package com.vacationary.backend.infra.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.dtos.assistant.BlLlmMessage;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;

class HttpLlmClientTest {

	@Test
	void blankApiKeyIsAnErrorWithoutCallingTheNetwork() {
		HttpLlmClient client = new HttpLlmClient(
				"https://example.invalid/v1",
				"gpt-4o-mini",
				"",
				new OpenAiChatMapper(new ObjectMapper()));
		BlLlmResponse response = client.complete(new BlLlmRequest(
				"system",
				List.of(new BlLlmMessage.User("Rome")),
				true));
		BlLlmResponse.Error error = assertInstanceOf(BlLlmResponse.Error.class, response);
		assertEquals("LLM is not configured", error.message());
	}
}
