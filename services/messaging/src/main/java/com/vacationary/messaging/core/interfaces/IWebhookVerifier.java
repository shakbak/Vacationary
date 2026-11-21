package com.vacationary.messaging.core.interfaces;

public interface IWebhookVerifier {

	boolean matches(String mode, String token);
}
