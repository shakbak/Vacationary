package com.vacationary.messaging.infra.telegram;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class TelegramUpdatePollerTest {

	@Test
	void blankTokenDoesNotStartPolling() {
		ObjectMapper objectMapper = new ObjectMapper();
		TelegramUpdateHandler handler = new TelegramUpdateHandler(
				new TelegramInboundParser(),
				inbound -> Optional.empty(),
				new TelegramBotClient("http://telegram", "", objectMapper));
		TelegramUpdatePoller poller = new TelegramUpdatePoller("", handler);
		poller.run(null);
		poller.destroy();
	}
}
