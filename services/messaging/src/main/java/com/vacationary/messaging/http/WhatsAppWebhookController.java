package com.vacationary.messaging.http;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vacationary.messaging.core.interfaces.IWhatsAppWebhookHandler;

@RestController
@RequestMapping("/webhooks/whatsapp")
public class WhatsAppWebhookController {

	private final IWhatsAppWebhookHandler webhookHandler;

	public WhatsAppWebhookController(IWhatsAppWebhookHandler webhookHandler) {
		this.webhookHandler = webhookHandler;
	}

	@GetMapping
	public ResponseEntity<String> verify(
			@RequestParam(name = "hub.mode", required = false) String mode,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String challenge) {
		String echo = webhookHandler.verify(mode, token, challenge);
		if (echo == null) {
			return ResponseEntity.status(403).build();
		}
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(echo);
	}

	@PostMapping
	public ResponseEntity<Void> inbound(@RequestBody(required = false) String rawBody) {
		webhookHandler.inbound(rawBody);
		return ResponseEntity.ok().build();
	}
}
