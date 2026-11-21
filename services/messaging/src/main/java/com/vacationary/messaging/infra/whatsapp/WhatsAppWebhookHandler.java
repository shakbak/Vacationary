package com.vacationary.messaging.infra.whatsapp;

import java.util.Optional;

import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.core.interfaces.IMessageSender;
import com.vacationary.messaging.core.interfaces.IWebhookVerifier;
import com.vacationary.messaging.core.interfaces.IWhatsAppWebhookHandler;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlOutboundMessage;

public class WhatsAppWebhookHandler implements IWhatsAppWebhookHandler {

	private final IWebhookVerifier verifier;
	private final IInboundParser inboundParser;
	private final IInboundReplyManager inboundReplyManager;
	private final IMessageSender messageSender;

	public WhatsAppWebhookHandler(
			IWebhookVerifier verifier,
			IInboundParser inboundParser,
			IInboundReplyManager inboundReplyManager,
			IMessageSender messageSender) {
		this.verifier = verifier;
		this.inboundParser = inboundParser;
		this.inboundReplyManager = inboundReplyManager;
		this.messageSender = messageSender;
	}

	@Override
	public String verify(String mode, String token, String challenge) {
		if (!verifier.matches(mode, token)) {
			return null;
		}
		return challenge == null ? "" : challenge;
	}

	@Override
	public void inbound(String rawBody) {
		for (BlInboundMessage message : inboundParser.parse(rawBody)) {
			Optional<BlOutboundMessage> outbound = inboundReplyManager.process(message);
			if (outbound.isEmpty()) {
				continue;
			}
			try {
				BlOutboundMessage reply = outbound.get();
				messageSender.send(reply.to(), reply.body());
			}
			catch (RuntimeException ignored) {
				// M-11: do not loop retries from here. Webhook still returns 200.
			}
		}
	}
}
