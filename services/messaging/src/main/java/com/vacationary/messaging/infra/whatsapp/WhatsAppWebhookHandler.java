package com.vacationary.messaging.infra.whatsapp;

import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.core.interfaces.ITurnProcessor;
import com.vacationary.messaging.core.interfaces.IWebhookVerifier;
import com.vacationary.messaging.core.interfaces.IWhatsAppWebhookHandler;
import com.vacationary.messaging.dtos.BlInboundMessage;

public class WhatsAppWebhookHandler implements IWhatsAppWebhookHandler {

	private final IWebhookVerifier verifier;
	private final IInboundParser inboundParser;
	private final ITurnProcessor turnProcessor;

	public WhatsAppWebhookHandler(
			IWebhookVerifier verifier,
			IInboundParser inboundParser,
			ITurnProcessor turnProcessor) {
		this.verifier = verifier;
		this.inboundParser = inboundParser;
		this.turnProcessor = turnProcessor;
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
			turnProcessor.process(message);
		}
	}
}
