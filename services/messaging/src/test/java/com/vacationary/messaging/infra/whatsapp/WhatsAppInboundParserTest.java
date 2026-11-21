package com.vacationary.messaging.infra.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.dtos.BlInboundMessage;

class WhatsAppInboundParserTest {

	private final WhatsAppInboundParser parser = new WhatsAppInboundParser(new ObjectMapper());

	@Test
	void readsTextMessagesAndIgnoresStatuses() {
		String json = """
				{
				  "object": "whatsapp_business_account",
				  "entry": [{
				    "changes": [{
				      "value": {
				        "messages": [{
				          "from": "15551234567",
				          "id": "wamid.HBgN",
				          "text": { "body": "hotels in Rome" },
				          "type": "text"
				        }],
				        "statuses": [{ "id": "wamid.other", "status": "delivered" }]
				      }
				    }]
				  }]
				}
				""";
		List<BlInboundMessage> messages = parser.parse(json);
		assertEquals(1, messages.size());
		assertEquals(Channels.WHATSAPP, messages.get(0).channel());
		assertEquals("15551234567", messages.get(0).externalUserId());
		assertEquals("hotels in Rome", messages.get(0).text());
		assertEquals("wamid.HBgN", messages.get(0).eventId());
	}

	@Test
	void ignoresNonTextAndBrokenJson() {
		assertTrue(parser.parse("""
				{"entry":[{"changes":[{"value":{"messages":[{"from":"1","id":"x","type":"image"}]}}]}]}
				""").isEmpty());
		assertTrue(parser.parse("not-json").isEmpty());
		assertTrue(parser.parse("").isEmpty());
	}
}
