package com.vacationary.messaging.infra.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.core.interfaces.IMessageSender;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.dtos.BlOutboundMessage;

class WhatsAppWebhookHandlerTest {

	@Test
	void inboundSendsRenderedReply() {
		List<String> sent = new ArrayList<>();
		WhatsAppWebhookHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "Where?")),
				(to, body) -> sent.add(to + "|" + body));
		handler.inbound("""
				{"entry":[{"changes":[{"value":{"messages":[{
				  "from":"15551234567","id":"wamid.1","type":"text","text":{"body":"a hotel"}}]}}]}]}
				""");
		assertEquals(List.of("15551234567|Where?"), sent);
	}

	@Test
	void inboundSkipsSendWhenProcessorReturnsEmpty() {
		List<String> sent = new ArrayList<>();
		WhatsAppWebhookHandler handler = handler(
				inbound -> Optional.empty(),
				(to, body) -> sent.add(to + "|" + body));
		handler.inbound("""
				{"entry":[{"changes":[{"value":{"messages":[{
				  "from":"15551234567","id":"wamid.1","type":"text","text":{"body":"a hotel"}}]}}]}]}
				""");
		assertTrue(sent.isEmpty());
	}

	@Test
	void sendFailureDoesNotPropagate() {
		WhatsAppWebhookHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "Where?")),
				(to, body) -> {
					throw new IllegalStateException("graph down");
				});
		handler.inbound("""
				{"entry":[{"changes":[{"value":{"messages":[{
				  "from":"15551234567","id":"wamid.1","type":"text","text":{"body":"hi"}}]}}]}]}
				""");
	}

	private static WhatsAppWebhookHandler handler(IInboundReplyManager inboundReplyManager, IMessageSender sender) {
		return new WhatsAppWebhookHandler(
				new WhatsAppWebhookVerifier("secret"),
				new WhatsAppInboundParser(new ObjectMapper()),
				inboundReplyManager,
				sender);
	}
}
