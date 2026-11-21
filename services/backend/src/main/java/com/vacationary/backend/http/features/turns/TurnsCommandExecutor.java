package com.vacationary.backend.http.features.turns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.conversation.IProcessedEventManager;
import com.vacationary.backend.core.interfaces.http.ICommandExecutor;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlMessage;
import com.vacationary.backend.dtos.conversation.BlProcessedEvent;
import com.vacationary.backend.dtos.conversation.BlTurnCommand;
import com.vacationary.backend.dtos.conversation.BlTurnResult;
import com.vacationary.backend.errors.bl.BlValidationError;

public class TurnsCommandExecutor implements ICommandExecutor<BlTurnCommand, BlTurnResult> {

	static final String ASSISTANT_FAIL = "I could not process that message. Please retry.";

	private final IProcessedEventManager processedEventManager;
	private final IConversationManager conversationManager;
	private final IAssistantManager assistantManager;

	public TurnsCommandExecutor(
			IProcessedEventManager processedEventManager,
			IConversationManager conversationManager,
			IAssistantManager assistantManager) {
		this.processedEventManager = processedEventManager;
		this.conversationManager = conversationManager;
		this.assistantManager = assistantManager;
	}

	@Override
	public BlTurnResult execute(BlTurnCommand command) {
		if (command == null || command.conversation() == null) {
			throw new BlValidationError("turn is required");
		}
		BlEventKey eventKey = new BlEventKey(command.conversation().channel(), command.eventId());
		Optional<BlProcessedEvent> existing = processedEventManager.getEvent(eventKey);
		if (existing.isPresent()) {
			return new BlTurnResult(existing.get().outcome(), true);
		}
		BlConversation history = conversationManager.getConversation(command.conversation());
		BlAssistantResponse response = respond(history, command.text());
		conversationManager.saveTurn(command.conversation(), command.text(), response);
		processedEventManager.save(new BlProcessedEvent(eventKey, response));
		return new BlTurnResult(response, false);
	}

	private BlAssistantResponse respond(BlConversation history, String text) {
		try {
			return assistantManager.respond(toAssistantRequest(history, text));
		}
		catch (RuntimeException ignored) {
			return BlAssistantResponse.fail(ASSISTANT_FAIL);
		}
	}

	private static BlAssistantRequest toAssistantRequest(BlConversation history, String text) {
		List<BlChatMessage> messages = new ArrayList<>();
		for (BlMessage message : history.messages()) {
			messages.add(new BlChatMessage(message.role(), message.text()));
		}
		messages.add(new BlChatMessage(ChatRole.USER, text));
		return new BlAssistantRequest(messages, text);
	}
}
