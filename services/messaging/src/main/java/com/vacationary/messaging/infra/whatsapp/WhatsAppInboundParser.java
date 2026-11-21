package com.vacationary.messaging.infra.whatsapp;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.dtos.BlInboundMessage;

public class WhatsAppInboundParser implements IInboundParser {

	private final ObjectMapper objectMapper;

	public WhatsAppInboundParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public List<BlInboundMessage> parse(String rawBody) {
		if (rawBody == null || rawBody.isBlank()) {
			return List.of();
		}
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			List<BlInboundMessage> messages = new ArrayList<>();
			for (JsonNode entry : root.path("entry")) {
				for (JsonNode change : entry.path("changes")) {
					JsonNode value = change.path("value");
					for (JsonNode message : value.path("messages")) {
						BlInboundMessage inbound = toInbound(message);
						if (inbound != null) {
							messages.add(inbound);
						}
					}
				}
			}
			return List.copyOf(messages);
		}
		catch (RuntimeException error) {
			return List.of();
		}
	}

	private static BlInboundMessage toInbound(JsonNode message) {
		if (!"text".equals(message.path("type").asText())) {
			return null;
		}
		String from = message.path("from").asText("").trim();
		String eventId = message.path("id").asText("").trim();
		String text = message.path("text").path("body").asText("").trim();
		if (from.isEmpty() || eventId.isEmpty() || text.isEmpty()) {
			return null;
		}
		return new BlInboundMessage(Channels.WHATSAPP, from, text, eventId);
	}
}
