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
				new BlInboundMessage(Channels.TELEGRAM, "4242", "a hotel", "9001"));
		assertEquals("4242", outbound.orElseThrow().to());
		assertEquals("Where would you like to stay?", outbound.orElseThrow().body());
	}

	@Test
	void skipsReplyOnDuplicate() {
		InboundReplyManager duplicates = new InboundReplyManager(
				inbound -> new BlTurnResult("clarify", "Where would you like to stay?", List.of(), true),
				new ReplyRenderer());
		assertTrue(duplicates.process(
				new BlInboundMessage(Channels.TELEGRAM, "4242", "a hotel", "9001")).isEmpty());
	}

	@Test
	void stillRepliesWhenBackendReturnsFail() {
		InboundReplyManager failing = new InboundReplyManager(
				inbound -> BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE),
				new ReplyRenderer());
		assertEquals(
				ReplyCopy.BACKEND_UNREACHABLE,
				failing.process(new BlInboundMessage(Channels.TELEGRAM, "4242", "Rome", "9001"))
						.orElseThrow()
						.body());
	}

	@Test
	void skipsBlankInbound() {
		assertTrue(manager.process(null).isEmpty());
		assertTrue(manager.process(new BlInboundMessage(Channels.TELEGRAM, "", "hi", "9001")).isEmpty());
		assertTrue(manager.process(new BlInboundMessage(Channels.TELEGRAM, "4242", "  ", "9001")).isEmpty());
	}
}
