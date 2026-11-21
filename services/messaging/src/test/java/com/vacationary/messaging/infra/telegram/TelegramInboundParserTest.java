package com.vacationary.messaging.infra.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.dtos.BlInboundMessage;

class TelegramInboundParserTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final TelegramInboundParser parser = new TelegramInboundParser();

	@Test
	void readsPrivateText() {
		List<BlInboundMessage> messages = parse("""
				{"update_id": 9001, "message": {
				  "from": {"id": 4242},
				  "chat": {"id": 4242, "type": "private"},
				  "text": "hotels in Rome"
				}}
				""");
		assertEquals(1, messages.size());
		assertEquals(Channels.TELEGRAM, messages.get(0).channel());
		assertEquals("4242", messages.get(0).externalUserId());
		assertEquals("hotels in Rome", messages.get(0).text());
		assertEquals("9001", messages.get(0).eventId());
	}

	@Test
	void readsStringIds() {
		List<BlInboundMessage> messages = parse("""
				{"update_id": "9001", "message": {
				  "from": {"id": "4242"},
				  "chat": {"id": "4242", "type": "private"},
				  "text": "hotels in Rome"
				}}
				""");
		assertEquals("4242", messages.get(0).externalUserId());
		assertEquals("9001", messages.get(0).eventId());
	}

	@Test
	void ignoresGroupsMediaAndMissingNodes() {
		assertTrue(parse("""
				{"update_id": 1, "message": {
				  "from": {"id": 1},
				  "chat": {"id": -100, "type": "group"},
				  "text": "hotels in Rome"
				}}
				""").isEmpty());
		assertTrue(parse("""
				{"update_id": 2, "message": {
				  "from": {"id": 1},
				  "chat": {"id": 1, "type": "private"},
				  "photo": [{}]
				}}
				""").isEmpty());
		assertTrue(parser.parse(null).isEmpty());
	}

	private List<BlInboundMessage> parse(String json) {
		JsonNode update = objectMapper.readTree(json);
		return parser.parse(update);
	}
}
