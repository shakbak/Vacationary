package com.vacationary.messaging.infra.telegram;

import java.util.List;

import tools.jackson.databind.JsonNode;
import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.dtos.BlInboundMessage;

public class TelegramInboundParser implements IInboundParser {

	@Override
	public List<BlInboundMessage> parse(JsonNode update) {
		if (update == null || update.isNull() || update.isMissingNode()) {
			return List.of();
		}
		try {
			BlInboundMessage inbound = toInbound(update);
			return inbound == null ? List.of() : List.of(inbound);
		}
		catch (RuntimeException error) {
			return List.of();
		}
	}

	private static BlInboundMessage toInbound(JsonNode update) {
		JsonNode message = update.get("message");
		if (message == null || message.isNull()) {
			return null;
		}
		if (!"private".equals(message.path("chat").path("type").asText())) {
			return null;
		}
		String from = idText(message.path("from").get("id"));
		String eventId = idText(update.get("update_id"));
		String text = message.path("text").asText("").trim();
		if (from.isEmpty() || eventId.isEmpty() || text.isEmpty()) {
			return null;
		}
		return new BlInboundMessage(Channels.TELEGRAM, from, text, eventId);
	}

	private static String idText(JsonNode node) {
		if (node == null || node.isNull() || !node.isValueNode()) {
			return "";
		}
		if (node.isNumber()) {
			return String.valueOf(node.asLong());
		}
		return node.asText("").trim();
	}
}
