package com.vacationary.messaging.infra.whatsapp;

import com.vacationary.messaging.core.interfaces.IWebhookVerifier;

public class WhatsAppWebhookVerifier implements IWebhookVerifier {

	private final String expectedToken;

	public WhatsAppWebhookVerifier(String expectedToken) {
		this.expectedToken = expectedToken == null ? "" : expectedToken;
	}

	@Override
	public boolean matches(String mode, String token) {
		if (expectedToken.isBlank()) {
			return false;
		}
		return "subscribe".equals(mode) && expectedToken.equals(token);
	}
}
