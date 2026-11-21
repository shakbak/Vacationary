package com.vacationary.messaging.infra.telegram;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.core.interfaces.IMessageSender;

public class TelegramBotClient implements IMessageSender {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String botToken;

	public TelegramBotClient(String apiBaseUrl, String botToken, ObjectMapper objectMapper) {
		this(apiBaseUrl, botToken, objectMapper, null);
	}

	TelegramBotClient(String apiBaseUrl, String botToken, ObjectMapper objectMapper, RestClient restClient) {
		this.objectMapper = objectMapper;
		this.botToken = botToken == null ? "" : botToken;
		this.restClient = restClient != null ? restClient : defaultClient(apiBaseUrl);
	}

	JsonNode getUpdates(long offset) {
		requireToken();
		String raw = restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/bot" + botToken + "/getUpdates")
						.queryParam("timeout", 30)
						.queryParam("offset", offset)
						.queryParam("allowed_updates", "[\"message\"]")
						.build())
				.retrieve()
				.body(String.class);
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException("Telegram getUpdates returned empty body");
		}
		JsonNode root = objectMapper.readTree(raw);
		if (!root.path("ok").asBoolean(false)) {
			throw new IllegalStateException("Telegram getUpdates failed");
		}
		return root;
	}

	@Override
	public void send(String to, String body) {
		requireToken();
		if (to == null || to.isBlank() || body == null || body.isBlank()) {
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(Map.of(
					"chat_id", to,
					"text", body));
			restClient.post()
					.uri("/bot" + botToken + "/sendMessage")
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RuntimeException error) {
			throw new IllegalStateException("Telegram send failed", error);
		}
	}

	private void requireToken() {
		if (botToken.isBlank()) {
			throw new IllegalStateException("Telegram is not configured");
		}
	}

	private static RestClient defaultClient(String baseUrl) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(45));
		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory)
				.build();
	}
}
