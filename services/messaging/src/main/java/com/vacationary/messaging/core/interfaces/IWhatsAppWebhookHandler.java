package com.vacationary.messaging.core.interfaces;

public interface IWhatsAppWebhookHandler {

	String verify(String mode, String token, String challenge);

	void inbound(String rawBody);
}
