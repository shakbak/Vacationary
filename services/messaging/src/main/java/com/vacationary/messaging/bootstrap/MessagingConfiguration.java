package com.vacationary.messaging.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.core.interfaces.IBackendClient;
import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.core.interfaces.IMessageSender;
import com.vacationary.messaging.core.interfaces.IReplyRenderer;
import com.vacationary.messaging.core.interfaces.IWebhookVerifier;
import com.vacationary.messaging.core.interfaces.IWhatsAppWebhookHandler;
import com.vacationary.messaging.domain.inbound.InboundReplyManager;
import com.vacationary.messaging.domain.render.ReplyRenderer;
import com.vacationary.messaging.infra.backend.HttpBackendClient;
import com.vacationary.messaging.infra.whatsapp.WhatsAppCloudClient;
import com.vacationary.messaging.infra.whatsapp.WhatsAppInboundParser;
import com.vacationary.messaging.infra.whatsapp.WhatsAppWebhookHandler;
import com.vacationary.messaging.infra.whatsapp.WhatsAppWebhookVerifier;

@Configuration
public class MessagingConfiguration {

	@Bean
	IReplyRenderer replyRenderer() {
		return new ReplyRenderer();
	}

	@Bean
	IBackendClient backendClient(
			@Value("${vacationary.backend.base-url}") String baseUrl,
			ObjectMapper objectMapper) {
		return new HttpBackendClient(baseUrl, objectMapper);
	}

	@Bean
	IMessageSender messageSender(
			@Value("${vacationary.whatsapp.graph-base-url}") String graphBaseUrl,
			@Value("${vacationary.whatsapp.access-token:}") String accessToken,
			@Value("${vacationary.whatsapp.phone-number-id:}") String phoneNumberId,
			ObjectMapper objectMapper) {
		return new WhatsAppCloudClient(graphBaseUrl, accessToken, phoneNumberId, objectMapper);
	}

	@Bean
	IInboundReplyManager inboundReplyManager(IBackendClient backendClient, IReplyRenderer replyRenderer) {
		return new InboundReplyManager(backendClient, replyRenderer);
	}

	@Bean
	IInboundParser inboundParser(ObjectMapper objectMapper) {
		return new WhatsAppInboundParser(objectMapper);
	}

	@Bean
	IWebhookVerifier webhookVerifier(
			@Value("${vacationary.whatsapp.verify-token:}") String verifyToken) {
		return new WhatsAppWebhookVerifier(verifyToken);
	}

	@Bean
	IWhatsAppWebhookHandler whatsAppWebhookHandler(
			IWebhookVerifier webhookVerifier,
			IInboundParser inboundParser,
			IInboundReplyManager inboundReplyManager,
			IMessageSender messageSender) {
		return new WhatsAppWebhookHandler(webhookVerifier, inboundParser, inboundReplyManager, messageSender);
	}
}
