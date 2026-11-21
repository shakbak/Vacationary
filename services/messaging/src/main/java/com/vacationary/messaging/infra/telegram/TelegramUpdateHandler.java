package com.vacationary.messaging.infra.telegram;

import java.util.Optional;

import tools.jackson.databind.JsonNode;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.core.interfaces.IInboundParser;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlOutboundMessage;

public class TelegramUpdateHandler {

	private final IInboundParser inboundParser;
	private final IInboundReplyManager inboundReplyManager;
	private final TelegramBotClient botClient;

	private long offset;

	public TelegramUpdateHandler(
			IInboundParser inboundParser,
			IInboundReplyManager inboundReplyManager,
			TelegramBotClient botClient) {
		this.inboundParser = inboundParser;
		this.inboundReplyManager = inboundReplyManager;
		this.botClient = botClient;
	}

	void poll() {
		consume(botClient.getUpdates(offset));
	}

	long consume(JsonNode root) {
		if (root == null) {
			return offset;
		}
		JsonNode result = root.path("result");
		if (!result.isArray()) {
			return offset;
		}
		long next = offset;
		for (JsonNode update : result) {
			long id = update.path("update_id").asLong(-1);
			if (id >= 0) {
				next = Math.max(next, id + 1);
			}
			deliver(update);
		}
		offset = next;
		return offset;
	}

	private void deliver(JsonNode update) {
		for (BlInboundMessage inbound : inboundParser.parse(update)) {
			Optional<BlOutboundMessage> outbound;
			try {
				outbound = inboundReplyManager.process(inbound);
			}
			catch (RuntimeException error) {
				sendQuietly(inbound.externalUserId(), ReplyCopy.BACKEND_UNREACHABLE);
				continue;
			}
			if (outbound.isEmpty()) {
				continue;
			}
			BlOutboundMessage reply = outbound.get();
			try {
				botClient.send(reply.to(), reply.body());
			}
			catch (RuntimeException error) {
				// One short fallback (covers oversize replies). Then give up (M-11).
				if (!ReplyCopy.DELIVERY_FAILED.equals(reply.body())) {
					sendQuietly(reply.to(), ReplyCopy.DELIVERY_FAILED);
				}
			}
		}
	}

	private void sendQuietly(String to, String body) {
		try {
			botClient.send(to, body);
		}
		catch (RuntimeException ignored) {
		}
	}
}
