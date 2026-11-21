package com.vacationary.backend.domain.conversation;

import java.time.Instant;
import java.util.List;

import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationRepository;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.BlMessage;
import com.vacationary.backend.errors.bl.BlValidationError;

public class ConversationManager implements IConversationManager {

	private final IConversationRepository conversationRepository;
	private final ConversationMapper conversationMapper;

	public ConversationManager(
			IConversationRepository conversationRepository,
			ConversationMapper conversationMapper) {
		this.conversationRepository = conversationRepository;
		this.conversationMapper = conversationMapper;
	}

	@Override
	public BlConversation getConversation(BlConversationKey key) {
		BlConversationKey normalized = ConversationValidator.validateKey(key);
		return load(normalized);
	}

	@Override
	public void saveTurn(BlConversationKey key, String text, BlAssistantResponse response) {
		BlConversationKey normalizedKey = ConversationValidator.validateKey(key);
		String normalizedText = ConversationValidator.validateText(text);
		ConversationValidator.validateResponse(response);
		BlConversation conversation = load(normalizedKey);
		BlMessage userMessage = new BlMessage(
				ChatRole.USER,
				normalizedText,
				Instant.now(),
				null,
				List.of());
		BlMessage assistantMessage = new BlMessage(
				ChatRole.ASSISTANT,
				response.text(),
				Instant.now(),
				response.kind(),
				response.recommendations());
		conversation = conversation.withMessage(userMessage).withMessage(assistantMessage);
		conversationRepository.save(conversationMapper.toDb(conversation));
	}

	private BlConversation load(BlConversationKey key) {
		return conversationRepository.find(key)
				.map(conversationMapper::toBl)
				.orElseGet(() -> BlConversation.empty(key));
	}

	private static final class ConversationValidator {

		private ConversationValidator() {
		}

		static BlConversationKey validateKey(BlConversationKey key) {
			if (key == null) {
				throw new BlValidationError("key is required");
			}
			if (key.channel() == null) {
				throw new BlValidationError("channel is required");
			}
			String externalUserId = trimToNull(key.externalUserId());
			if (externalUserId == null) {
				throw new BlValidationError("externalUserId is required");
			}
			return new BlConversationKey(key.channel(), externalUserId);
		}

		static String validateText(String text) {
			String normalized = trimToNull(text);
			if (normalized == null) {
				throw new BlValidationError("text is required");
			}
			return normalized;
		}

		static void validateResponse(BlAssistantResponse response) {
			if (response == null || response.kind() == null) {
				throw new BlValidationError("assistant response is required");
			}
			if (trimToNull(response.text()) == null) {
				throw new BlValidationError("assistant response text is required");
			}
		}

		private static String trimToNull(String value) {
			if (value == null) {
				return null;
			}
			String trimmed = value.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
	}
}
