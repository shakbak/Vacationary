package com.vacationary.messaging.domain.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlTurnResult;
import com.vacationary.messaging.domain.render.ReplyRenderer;

class TurnProcessorTest {

	@Test
	void sendsRenderedReplyWhenBackendSucceeds() {
		RecordingSender sender = new RecordingSender();
		TurnProcessor processor = new TurnProcessor(
				inbound -> new BlTurnResult("clarify", "Where would you like to stay?", List.of(), false),
				new ReplyRenderer(),
				sender);
		processor.process(new BlInboundMessage(Channels.WHATSAPP, "15551234567", "a hotel", "wamid.1"));
		assertEquals(List.of("15551234567"), sender.recipients);
		assertEquals(List.of("Where would you like to stay?"), sender.bodies);
	}

	@Test
	void skipsSendOnDuplicate() {
		RecordingSender sender = new RecordingSender();
		TurnProcessor processor = new TurnProcessor(
				inbound -> new BlTurnResult("clarify", "Where would you like to stay?", List.of(), true),
				new ReplyRenderer(),
				sender);
		processor.process(new BlInboundMessage(Channels.WHATSAPP, "15551234567", "a hotel", "wamid.1"));
		assertTrue(sender.bodies.isEmpty());
	}

	@Test
	void stillSendsWhenBackendReturnsFail() {
		RecordingSender sender = new RecordingSender();
		TurnProcessor processor = new TurnProcessor(
				inbound -> BlTurnResult.fail(ReplyCopy.BACKEND_UNREACHABLE),
				new ReplyRenderer(),
				sender);
		processor.process(new BlInboundMessage(Channels.WHATSAPP, "15551234567", "Rome", "wamid.1"));
		assertEquals(List.of(ReplyCopy.BACKEND_UNREACHABLE), sender.bodies);
	}

	@Test
	void sendFailureDoesNotPropagate() {
		TurnProcessor processor = new TurnProcessor(
				inbound -> new BlTurnResult("clarify", "Where?", List.of(), false),
				new ReplyRenderer(),
				(to, body) -> {
					throw new IllegalStateException("graph down");
				});
		processor.process(new BlInboundMessage(Channels.WHATSAPP, "15551234567", "hi", "wamid.1"));
	}

	private static final class RecordingSender implements com.vacationary.messaging.core.interfaces.IMessageSender {
		private final List<String> recipients = new ArrayList<>();
		private final List<String> bodies = new ArrayList<>();

		@Override
		public void send(String to, String body) {
			recipients.add(to);
			bodies.add(body);
		}
	}
}
