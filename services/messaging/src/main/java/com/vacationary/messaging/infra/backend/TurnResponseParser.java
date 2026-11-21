package com.vacationary.messaging.infra.backend;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.dtos.BlStayOption;
import com.vacationary.messaging.dtos.BlTurnResult;

public class TurnResponseParser {

	private final ObjectMapper objectMapper;

	public TurnResponseParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public BlTurnResult parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE);
		}
		try {
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
		catch (RuntimeException error) {
			return BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE);
		}
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
}
