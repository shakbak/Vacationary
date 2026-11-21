package com.vacationary.messaging.infra.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.dtos.BlOutboundMessage;

class TelegramUpdateHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void privateTextSendsRenderedReplyAndAdvancesOffset() {
		RecordingBotClient bot = recordingBot();
		TelegramUpdateHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "Where?")),
				bot);
		long offset = handler.consume(objectMapper.readTree("""
				{"ok":true,"result":[{"update_id":10,"message":{
				  "from":{"id":4242},"chat":{"id":4242,"type":"private"},"text":"a hotel"}}]}
				"""));
		assertEquals(11, offset);
		assertEquals(List.of("4242|Where?"), bot.sent);
	}

	@Test
	void skipsSendWhenManagerReturnsEmptyButStillAdvancesOffset() {
		RecordingBotClient bot = recordingBot();
		TelegramUpdateHandler handler = handler(inbound -> Optional.empty(), bot);
		long offset = handler.consume(objectMapper.readTree("""
				{"ok":true,"result":[{"update_id":10,"message":{
				  "from":{"id":4242},"chat":{"id":4242,"type":"private"},"text":"a hotel"}}]}
				"""));
		assertEquals(11, offset);
		assertTrue(bot.sent.isEmpty());
	}

	@Test
	void groupUpdatesAdvanceOffsetWithoutSend() {
		RecordingBotClient bot = recordingBot();
		TelegramUpdateHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "nope")),
				bot);
		long offset = handler.consume(objectMapper.readTree("""
				{"ok":true,"result":[{"update_id":7,"message":{
				  "from":{"id":1},"chat":{"id":-100,"type":"group"},"text":"hotels"}}]}
				"""));
		assertEquals(8, offset);
		assertTrue(bot.sent.isEmpty());
	}

	@Test
	void mixedBatchAdvancesPastSkippedUpdates() {
		RecordingBotClient bot = recordingBot();
		TelegramUpdateHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "Where?")),
				bot);
		long offset = handler.consume(objectMapper.readTree("""
				{"ok":true,"result":[
				  {"update_id":4,"message":{"from":{"id":1},"chat":{"id":-9,"type":"group"},"text":"x"}},
				  {"update_id":5,"message":{"from":{"id":4242},"chat":{"id":4242,"type":"private"},"text":"Rome"}}
				]}
				"""));
		assertEquals(6, offset);
		assertEquals(List.of("4242|Where?"), bot.sent);
	}

	@Test
	void sendFailureSendsShortFallback() {
		RecordingBotClient bot = recordingBot();
		bot.failFirstSend = true;
		TelegramUpdateHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "Where?")),
				bot);
		handler.consume(objectMapper.readTree("""
				{"ok":true,"result":[{"update_id":3,"message":{
				  "from":{"id":9},"chat":{"id":9,"type":"private"},"text":"hi"}}]}
				"""));
		assertEquals(List.of("9|" + ReplyCopy.DELIVERY_FAILED), bot.sent);
	}

	@Test
	void sendFailureDoesNotPropagateWhenFallbackAlsoFails() {
		RecordingBotClient bot = recordingBot();
		bot.failAllSends = true;
		TelegramUpdateHandler handler = handler(
				inbound -> Optional.of(new BlOutboundMessage(inbound.externalUserId(), "Where?")),
				bot);
		handler.consume(objectMapper.readTree("""
				{"ok":true,"result":[{"update_id":3,"message":{
				  "from":{"id":9},"chat":{"id":9,"type":"private"},"text":"hi"}}]}
				"""));
		assertTrue(bot.sent.isEmpty());
	}

	private TelegramUpdateHandler handler(IInboundReplyManager manager, TelegramBotClient botClient) {
		return new TelegramUpdateHandler(new TelegramInboundParser(), manager, botClient);
	}

	private RecordingBotClient recordingBot() {
		return new RecordingBotClient(objectMapper);
	}

	private static final class RecordingBotClient extends TelegramBotClient {

		private final List<String> sent = new ArrayList<>();
		private int sendCalls;
		private boolean failFirstSend;
		private boolean failAllSends;

		private RecordingBotClient(ObjectMapper objectMapper) {
			super("http://telegram", "token", objectMapper);
		}

		@Override
		public void send(String to, String body) {
			sendCalls++;
			if (failAllSends || (failFirstSend && sendCalls == 1)) {
				throw new IllegalStateException("telegram down");
			}
			sent.add(to + "|" + body);
		}
	}
}
