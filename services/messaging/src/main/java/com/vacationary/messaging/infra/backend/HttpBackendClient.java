package com.vacationary.messaging.infra.backend;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.core.interfaces.IBackendClient;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlStayOption;
import com.vacationary.messaging.dtos.BlTurnResult;

public class HttpBackendClient implements IBackendClient {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public HttpBackendClient(String baseUrl, ObjectMapper objectMapper) {
		this(baseUrl, objectMapper, null);
	}

	HttpBackendClient(String baseUrl, ObjectMapper objectMapper, RestClient restClient) {
		this.objectMapper = objectMapper;
		this.restClient = restClient != null ? restClient : defaultClient(baseUrl);
	}

	@Override
	public BlTurnResult postTurn(BlInboundMessage inbound) {
		if (inbound == null) {
			return BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE);
		}
		try {
			String payload = objectMapper.writeValueAsString(Map.of(
					"channel", inbound.channel() == null ? "" : inbound.channel(),
					"externalUserId", inbound.externalUserId() == null ? "" : inbound.externalUserId(),
					"text", inbound.text() == null ? "" : inbound.text(),
					"eventId", inbound.eventId() == null ? "" : inbound.eventId()));
			String raw = restClient.post()
					.uri("/turns")
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);
			return parse(raw);
		}
		catch (RuntimeException error) {
			return BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE);
		}
	}

	private BlTurnResult parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE);
		}
		JsonNode node = objectMapper.readTree(raw);
		String outcome = text(node, "outcome");
		if (outcome.isEmpty()) {
			return BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE);
		}
		return new BlTurnResult(
				outcome,
				text(node, "text"),
				stays(node.get("recommendations")),
				node.path("duplicate").asBoolean(false));
	}

	private static List<BlStayOption> stays(JsonNode array) {
		if (array == null || !array.isArray()) {
			return List.of();
		}
		List<BlStayOption> options = new ArrayList<>();
		for (JsonNode row : array) {
			options.add(new BlStayOption(
					text(row, "name"),
					text(row, "stayType"),
					text(row, "location"),
					text(row, "price"),
					text(row, "rating"),
					text(row, "why"),
					text(row, "url")));
		}
		return options;
	}

	private static String text(JsonNode node, String field) {
		if (node == null || node.get(field) == null || node.get(field).isNull()) {
			return "";
		}
		return node.get(field).asText("");
	}

	private static RestClient defaultClient(String baseUrl) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(60));
		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory)
				.build();
	}
}
