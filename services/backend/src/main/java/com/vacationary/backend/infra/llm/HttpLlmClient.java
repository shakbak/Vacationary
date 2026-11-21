package com.vacationary.backend.infra.llm;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import com.vacationary.backend.core.interfaces.assistant.ILlmClient;
import com.vacationary.backend.core.interfaces.assistant.IOpenAiChatMapper;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.infra.http.BaseRestClient;

public class HttpLlmClient extends BaseRestClient implements ILlmClient {

	private final String model;
	private final String apiKey;
	private final IOpenAiChatMapper chatMapper;

	public HttpLlmClient(String baseUrl, String model, String apiKey, IOpenAiChatMapper chatMapper) {
		this(baseUrl, model, apiKey, chatMapper, null);
	}

	HttpLlmClient(String baseUrl, String model, String apiKey, IOpenAiChatMapper chatMapper, RestClient restClient) {
		super(baseUrl, apiKey, restClient);
		this.model = model;
		this.apiKey = apiKey == null ? "" : apiKey;
		this.chatMapper = chatMapper;
	}

	@Override
	public BlLlmResponse complete(BlLlmRequest request) {
		if (apiKey.isBlank()) {
			return new BlLlmResponse.Error("LLM is not configured");
		}
		if (request == null) {
			return new BlLlmResponse.Error("LLM request is required");
		}
		try {
			String payload = chatMapper.toRequestJson(request, model);
			String raw = restClient.post()
					.uri("/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);
			return chatMapper.toResponse(raw);
		}
		catch (JacksonException error) {
			return new BlLlmResponse.Error("LLM response was not valid JSON");
		}
		catch (RestClientResponseException error) {
			try {
				return chatMapper.toResponse(error.getResponseBodyAsString());
			}
			catch (RuntimeException ignored) {
				return new BlLlmResponse.Error("LLM request failed");
			}
		}
		catch (RestClientException error) {
			return new BlLlmResponse.Error("LLM request failed");
		}
	}
}
