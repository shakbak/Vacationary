package com.vacationary.backend.domain.assistant;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.vacationary.backend.core.interfaces.assistant.ILlmPromptBuilder;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.BlLlmMessage;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;

public class LlmPromptBuilder implements ILlmPromptBuilder {

	private final ObjectMapper objectMapper;

	public LlmPromptBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public BlLlmRequest build(BlAssistantRequest request) {
		List<BlLlmMessage> messages = new ArrayList<>();
		if (request != null) {
			for (BlChatMessage message : request.messages()) {
				if (message == null || message.text() == null) {
					continue;
				}
				if (message.role() == ChatRole.USER) {
					messages.add(new BlLlmMessage.User(message.text()));
				}
				else {
					messages.add(new BlLlmMessage.Assistant(message.text()));
				}
			}
			boolean shouldUseLatestUserTextAsOnlyMessage = messages.isEmpty()
					&& request.latestUserText() != null
					&& !request.latestUserText().isBlank();
			if (shouldUseLatestUserTextAsOnlyMessage) {
				messages.add(new BlLlmMessage.User(request.latestUserText()));
			}
		}
		return new BlLlmRequest(SystemPrompt.text(), messages, true);
	}

	@Override
	public BlLlmRequest withToolResult(
			BlLlmRequest original,
			BlLlmResponse.ToolRequest toolRequest,
			StaySearchResult.Success success) {
		List<BlLlmMessage> messages = new ArrayList<>(original.messages());
		String id = toolRequest.id() == null || toolRequest.id().isBlank() ? STAY_SEARCH_TOOL + "_1" : toolRequest.id();
		messages.add(new BlLlmMessage.AssistantToolCall(id, STAY_SEARCH_TOOL, toolRequest.jsonArgs()));
		messages.add(new BlLlmMessage.Tool(id, serializeOptions(success.options())));
		return new BlLlmRequest(original.systemPrompt(), messages, false);
	}

	private String serializeOptions(List<BlStayOption> options) {
		ArrayNode array = objectMapper.createArrayNode();
		for (BlStayOption option : options) {
			ObjectNode node = array.addObject();
			node.put("name", option.name());
			node.put("stayType", option.stayType() == null ? null : option.stayType().name());
			node.put("location", option.location());
			node.put("price", option.price());
			node.put("rating", option.rating());
			node.put("why", option.why());
			node.put("url", option.url());
		}
		try {
			return objectMapper.writeValueAsString(array);
		}
		catch (JacksonException error) {
			return "[]";
		}
	}

	static final class SystemPrompt {

		static final String ROLE = "You are Vacationary, a lodging recommendation assistant.";

		static final String LANGUAGE = """
				Language: English only. If the user writes in another language, reply:
				CLARIFY: I only support English. Please tell me the city where you want to stay.""";

		static final String SCOPE = """
				Scope: lodging only (hotel, motel, vacation rental, hostel, B&B, resort).
				If the user asks for flights, tours, restaurants, or other non-lodging help, reply:
				REFUSE_SCOPE: I only help with places to stay. Tell me a city and I can recommend lodging.""";

		static final String DESTINATION = """
				Destination is required. If you do not know the city from this message or earlier messages in this conversation, reply:
				CLARIFY: <one question asking where they want to stay>
				Do not ask for dates, budget, number of people, stay type, or amenities as a requirement. Those are optional. Ask at most one question.""";

		static final String TOOL_USE = """
				When the destination is known, call the stay_search tool. Pass destination and any optional filters the user already stated in this conversation (dates, budgetMax, guestCount, stayType, preferences).""";

		static final String FOLLOW_UPS = """
				Follow-ups: if the user said "hotels in Lisbon" and later "cheaper", call stay_search with destination Lisbon and a tighter budget. Take destination and filters from earlier messages in this conversation. Do not drop a city already given unless the user names a new city or asks to start over.""";

		static final String NO_INVENTED_STAYS = """
				Never invent stays. After stay_search returns, write a short English reply that presents those exact five options (name, type, location, price, rating, why, and the OTA URL). Do not add extra properties.""";

		static final String TEXT_REPLY_PREFIXES = """
				For replies that do not call a tool you MUST start with CLARIFY: or REFUSE_SCOPE: as shown above.""";

		private SystemPrompt() {
		}

		static String text() {
			return String.join("\n\n",
					ROLE,
					LANGUAGE,
					SCOPE,
					DESTINATION,
					TOOL_USE,
					FOLLOW_UPS,
					NO_INVENTED_STAYS,
					TEXT_REPLY_PREFIXES);
		}
	}
}
