package com.vacationary.backend.http.spring;

import java.util.Locale;

import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.errors.bl.BlValidationError;

public final class HttpChannels {

	private HttpChannels() {
	}

	public static ConversationChannel parse(String raw) {
		ConversationChannel channel = tryParse(raw);
		if (channel == null) {
			throw new BlValidationError("channel is invalid");
		}
		return channel;
	}

	public static boolean isSupported(String raw) {
		return tryParse(raw) != null;
	}

	public static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static ConversationChannel tryParse(String raw) {
		String normalized = trimToNull(raw);
		if (normalized == null) {
			return null;
		}
		return switch (normalized.toLowerCase(Locale.ROOT)) {
			case "web" -> ConversationChannel.WEB;
			case "whatsapp" -> ConversationChannel.WHATSAPP;
			default -> null;
		};
	}
}
