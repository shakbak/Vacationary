package com.vacationary.backend.infra.llm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.vacationary.backend.core.interfaces.assistant.ILlmPromptBuilder;
import com.vacationary.backend.core.interfaces.assistant.IOpenAiChatMapper;
import com.vacationary.backend.dtos.assistant.BlLlmMessage;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;

public class OpenAiChatMapper implements IOpenAiChatMapper {

	private final ObjectMapper objectMapper;

	public OpenAiChatMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String toRequestJson(BlLlmRequest request, String model) {
		return objectMapper.writeValueAsString(toRequestBody(request, model));
	}

	@Override
	public BlLlmResponse toResponse(String rawJson) {
		return toResponse(objectMapper.readTree(rawJson));
	}

	private ObjectNode toRequestBody(BlLlmRequest request, String model) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", model);
		ArrayNode messages = body.putArray("messages");
		ObjectNode system = messages.addObject();
		system.put("role", "system");
		system.put("content", request.systemPrompt());
		for (BlLlmMessage message : request.messages()) {
			messages.add(toOpenAiMessage(message));
		}
		if (request.includeStaySearchTool()) {
			body.set("tools", tools());
		}
		return body;
	}

	private BlLlmResponse toResponse(JsonNode root) {
		if (root == null || root.isMissingNode()) {
			return new BlLlmResponse.Error("LLM returned an empty response");
		}
		if (root.hasNonNull("error")) {
			JsonNode error = root.get("error");
			String message = error.path("message").asText("LLM request failed");
			return new BlLlmResponse.Error(message);
		}
		JsonNode choice = root.path("choices").path(0).path("message");
		if (choice.isMissingNode() || choice.isNull()) {
			return new BlLlmResponse.Error("LLM returned no choices");
		}
		JsonNode toolCalls = choice.get("tool_calls");
		if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
			JsonNode call = toolCalls.get(0);
			String name = call.path("function").path("name").asText("");
			String args = call.path("function").path("arguments").asText("{}");
			String id = call.path("id").asText("");
			return new BlLlmResponse.ToolRequest(name, args, id);
		}
		JsonNode content = choice.get("content");
		if (content == null || content.isNull()) {
			return new BlLlmResponse.Error("LLM returned no content");
		}
		return new BlLlmResponse.Text(content.asText());
	}

	private ObjectNode toOpenAiMessage(BlLlmMessage message) {
		ObjectNode node = objectMapper.createObjectNode();
		return switch (message) {
			case BlLlmMessage.User user -> {
				node.put("role", "user");
				node.put("content", user.content());
				yield node;
			}
			case BlLlmMessage.Assistant assistant -> {
				node.put("role", "assistant");
				node.put("content", assistant.content());
				yield node;
			}
			case BlLlmMessage.AssistantToolCall toolCall -> {
				node.put("role", "assistant");
				ArrayNode calls = node.putArray("tool_calls");
				ObjectNode call = calls.addObject();
				call.put("id", toolCall.id());
				call.put("type", "function");
				ObjectNode function = call.putObject("function");
				function.put("name", toolCall.name());
				function.put("arguments", toolCall.jsonArgs());
				yield node;
			}
			case BlLlmMessage.Tool tool -> {
				node.put("role", "tool");
				node.put("tool_call_id", tool.toolCallId());
				node.put("content", tool.content());
				yield node;
			}
		};
	}

	private ArrayNode tools() {
		ArrayNode tools = objectMapper.createArrayNode();
		ObjectNode tool = tools.addObject();
		tool.put("type", "function");
		ObjectNode function = tool.putObject("function");
		function.put("name", ILlmPromptBuilder.STAY_SEARCH_TOOL);
		function.put("description", "Search for five lodging options in a destination.");
		ObjectNode parameters = function.putObject("parameters");
		parameters.put("type", "object");
		ObjectNode properties = parameters.putObject("properties");
		property(properties, "destination", "string", "City or place name. Required.");
		property(properties, "checkIn", "string", "Check-in date, ISO-8601 (YYYY-MM-DD).");
		property(properties, "checkOut", "string", "Check-out date, ISO-8601 (YYYY-MM-DD).");
		property(properties, "budgetMax", "integer", "Maximum price per night.");
		property(properties, "guestCount", "integer", "Total number of people.");
		property(properties, "stayType", "string", "hotel, motel, vacation_rental, hostel, bnb, or resort.");
		property(properties, "preferences", "string", "Amenities or free-text preferences.");
		parameters.putArray("required").add("destination");
		return tools;
	}

	private static void property(ObjectNode properties, String name, String type, String description) {
		ObjectNode node = properties.putObject(name);
		node.put("type", type);
		node.put("description", description);
	}
}
