package com.vacationary.backend.domain.conversation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationRepository;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.BlMessage;
import com.vacationary.backend.errors.bl.BlValidationError;

public class ConversationManager implements IConversationManager {

	static final String ASSISTANT_FAIL = "I could not process that message. Please retry.";

	private final IConversationRepository conversationRepository;
	private final ConversationMapper conversationMapper;
	private final IAssistantManager assistantManager;

	public ConversationManager(
			IConversationRepository conversationRepository,
			ConversationMapper conversationMapper,
			IAssistantManager assistantManager) {
		this.conversationRepository = conversationRepository;
		this.conversationMapper = conversationMapper;
		this.assistantManager = assistantManager;
	}

	@Override
	public BlAssistantResponse handleTurn(BlConversationKey key, String text) {
		BlConversationKey normalizedKey = ConversationValidator.validateKey(key);
		String normalizedText = ConversationValidator.validateText(text);
		BlConversation conversation = conversationRepository.find(normalizedKey)
				.map(conversationMapper::toBl)
				.orElseGet(() -> BlConversation.empty(normalizedKey));
		BlMessage userMessage = new BlMessage(
				ChatRole.USER,
				normalizedText,
				Instant.now(),
				null,
				List.of());
		conversation = conversation.withMessage(userMessage);
		BlAssistantResponse response = respond(conversation, normalizedText);
		BlMessage assistantMessage = new BlMessage(
				ChatRole.ASSISTANT,
				response.text(),
				Instant.now(),
				response.kind(),
				response.recommendations());
		conversation = conversation.withMessage(assistantMessage);
		conversationRepository.save(conversationMapper.toDb(conversation));
		return response;
	}

	@Override
	public BlConversation getConversation(BlConversationKey key) {
		BlConversationKey normalized = ConversationValidator.validateKey(key);
		return conversationRepository.find(normalized)
				.map(conversationMapper::toBl)
				.orElseGet(() -> BlConversation.empty(normalized));
	}

	private BlAssistantResponse respond(BlConversation conversation, String latestUserText) {
		try {
			List<BlChatMessage> messages = new ArrayList<>();
			for (BlMessage message : conversation.messages()) {
				messages.add(new BlChatMessage(message.role(), message.text()));
			}
			return assistantManager.respond(new BlAssistantRequest(messages, latestUserText));
		}
		catch (RuntimeException ignored) {
			return BlAssistantResponse.fail(ASSISTANT_FAIL);
		}
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

		private static String trimToNull(String value) {
			if (value == null) {
				return null;
			}
			String trimmed = value.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
	}
}
