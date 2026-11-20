package com.vacationary.backend.domain.assistant;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.StayType;

public class StaySearchParser {

	public sealed interface Result {

		record ParsedQuery(BlStaySearchQuery query) implements Result {
		}

		record MissingDestination() implements Result {
		}

		record InvalidArguments(String message) implements Result {
		}
	}

	private final ObjectMapper objectMapper;

	public StaySearchParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Result parse(String jsonArgs) {
		if (jsonArgs == null || jsonArgs.isBlank()) {
			return new Result.MissingDestination();
		}
		try {
			JsonNode root = objectMapper.readTree(jsonArgs);
			if (root == null || !root.isObject()) {
				return new Result.InvalidArguments("stay_search arguments must be a JSON object");
			}
			String destination = text(root, "destination");
			if (destination == null || destination.isBlank()) {
				return new Result.MissingDestination();
			}
			return new Result.ParsedQuery(new BlStaySearchQuery(
					destination.trim(),
					date(root, "checkIn"),
					date(root, "checkOut"),
					integer(root, "budgetMax"),
					integer(root, "guestCount"),
					stayType(root),
					text(root, "preferences")));
		}
		catch (JacksonException error) {
			return new Result.InvalidArguments("stay_search arguments are not valid JSON");
		}
	}

	private static String text(JsonNode root, String field) {
		JsonNode node = root.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		String value = node.asText();
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static Integer integer(JsonNode root, String field) {
		JsonNode node = root.get(field);
		if (node == null || node.isNull() || node.isTextual() && node.asText().isBlank()) {
			return null;
		}
		if (node.isNumber()) {
			return node.intValue();
		}
		try {
			return Integer.parseInt(node.asText().trim());
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static LocalDate date(JsonNode root, String field) {
		String raw = text(root, field);
		if (raw == null) {
			return null;
		}
		try {
			return LocalDate.parse(raw);
		}
		catch (DateTimeParseException ignored) {
			return null;
		}
	}

	private static StayType stayType(JsonNode root) {
		String raw = text(root, "stayType");
		if (raw == null) {
			return null;
		}
		String key = raw.toUpperCase(Locale.ROOT)
				.replace(' ', '_')
				.replace('-', '_')
				.replace("&", "");
		if (key.equals("BB") || key.equals("BANDB") || key.equals("BED_AND_BREAKFAST")) {
			return StayType.BNB;
		}
		try {
			return StayType.valueOf(key);
		}
		catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
