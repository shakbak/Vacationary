package com.vacationary.messaging.domain.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlOutboundMessage;
import com.vacationary.messaging.dtos.BlTurnResult;
import com.vacationary.messaging.domain.render.ReplyRenderer;

class InboundReplyManagerTest {

	private final InboundReplyManager manager = new InboundReplyManager(
			inbound -> new BlTurnResult("clarify", "Where would you like to stay?", List.of(), false),
			new ReplyRenderer());

	@Test
	void returnsRenderedReplyWhenBackendSucceeds() {
		Optional<BlOutboundMessage> outbound = manager.process(
				new BlInboundMessage(Channels.WHATSAPP, "15551234567", "a hotel", "wamid.1"));
		assertEquals("15551234567", outbound.orElseThrow().to());
		assertEquals("Where would you like to stay?", outbound.orElseThrow().body());
	}

	@Test
	void skipsReplyOnDuplicate() {
		InboundReplyManager duplicates = new InboundReplyManager(
				inbound -> new BlTurnResult("clarify", "Where would you like to stay?", List.of(), true),
				new ReplyRenderer());
		assertTrue(duplicates.process(
				new BlInboundMessage(Channels.WHATSAPP, "15551234567", "a hotel", "wamid.1")).isEmpty());
	}

	@Test
	void stillRepliesWhenBackendReturnsFail() {
		InboundReplyManager failing = new InboundReplyManager(
				inbound -> BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE),
				new ReplyRenderer());
		assertEquals(
				ReplyCopy.BACKEND_UNREACHABLE,
				failing.process(new BlInboundMessage(Channels.WHATSAPP, "15551234567", "Rome", "wamid.1"))
						.orElseThrow()
						.body());
	}

	@Test
	void skipsBlankInbound() {
		assertTrue(manager.process(null).isEmpty());
		assertTrue(manager.process(new BlInboundMessage(Channels.WHATSAPP, "", "hi", "wamid.1")).isEmpty());
		assertTrue(manager.process(new BlInboundMessage(Channels.WHATSAPP, "1555", "  ", "wamid.1")).isEmpty());
	}
}
