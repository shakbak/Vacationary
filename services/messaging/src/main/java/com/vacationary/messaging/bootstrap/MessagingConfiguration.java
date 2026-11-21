package com.vacationary.messaging.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.core.interfaces.IBackendClient;
import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.core.interfaces.IReplyRenderer;
import com.vacationary.messaging.domain.inbound.InboundReplyManager;
import com.vacationary.messaging.domain.render.ReplyRenderer;
import com.vacationary.messaging.infra.backend.HttpBackendClient;
import com.vacationary.messaging.infra.telegram.TelegramBotClient;
import com.vacationary.messaging.infra.telegram.TelegramInboundParser;
import com.vacationary.messaging.infra.telegram.TelegramUpdateHandler;
import com.vacationary.messaging.infra.telegram.TelegramUpdatePoller;

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
	TelegramBotClient telegramBotClient(
			@Value("${vacationary.telegram.api-base-url}") String apiBaseUrl,
			@Value("${vacationary.telegram.bot-token:}") String botToken,
			ObjectMapper objectMapper) {
		return new TelegramBotClient(apiBaseUrl, botToken, objectMapper);
	}

	@Bean
	IInboundReplyManager inboundReplyManager(IBackendClient backendClient, IReplyRenderer replyRenderer) {
		return new InboundReplyManager(backendClient, replyRenderer);
	}

	@Bean
	IInboundParser inboundParser() {
		return new TelegramInboundParser();
	}

	@Bean
	TelegramUpdateHandler telegramUpdateHandler(
			IInboundParser inboundParser,
			IInboundReplyManager inboundReplyManager,
			TelegramBotClient telegramBotClient) {
		return new TelegramUpdateHandler(inboundParser, inboundReplyManager, telegramBotClient);
	}

	@Bean
	TelegramUpdatePoller telegramUpdatePoller(
			@Value("${vacationary.telegram.bot-token:}") String botToken,
			TelegramUpdateHandler telegramUpdateHandler) {
		return new TelegramUpdatePoller(botToken, telegramUpdateHandler);
	}
}
