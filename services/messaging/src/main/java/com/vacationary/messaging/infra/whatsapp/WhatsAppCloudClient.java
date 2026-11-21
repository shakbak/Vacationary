package com.vacationary.messaging.infra.whatsapp;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.core.interfaces.IMessageSender;

public class WhatsAppCloudClient implements IMessageSender {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String accessToken;
	private final String phoneNumberId;

	public WhatsAppCloudClient(
			String graphBaseUrl,
			String accessToken,
			String phoneNumberId,
			ObjectMapper objectMapper) {
		this(graphBaseUrl, accessToken, phoneNumberId, objectMapper, null);
	}

	WhatsAppCloudClient(
			String graphBaseUrl,
			String accessToken,
			String phoneNumberId,
			ObjectMapper objectMapper,
			RestClient restClient) {
		this.objectMapper = objectMapper;
		this.accessToken = accessToken == null ? "" : accessToken;
		this.phoneNumberId = phoneNumberId == null ? "" : phoneNumberId;
		this.restClient = restClient != null ? restClient : defaultClient(graphBaseUrl, this.accessToken);
	}

	@Override
	public void send(String to, String body) {
		if (accessToken.isBlank() || phoneNumberId.isBlank()) {
			throw new IllegalStateException("WhatsApp is not configured");
		}
		if (to == null || to.isBlank() || body == null || body.isBlank()) {
			return;
		}
		try {
			String payload = objectMapper.writeValueAsString(Map.of(
					"messaging_product", "whatsapp",
					"to", to,
					"type", "text",
					"text", Map.of(
							"preview_url", true,
							"body", body)));
			restClient.post()
					.uri("/{phoneNumberId}/messages", phoneNumberId)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RuntimeException error) {
			throw new IllegalStateException("WhatsApp send failed", error);
		}
	}

	private static RestClient defaultClient(String baseUrl, String accessToken) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(20));
		RestClient.Builder builder = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory);
		if (!accessToken.isBlank()) {
			builder.defaultHeader("Authorization", "Bearer " + accessToken);
		}
		return builder.build();
	}
}
