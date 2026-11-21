package com.vacationary.backend.http.features.history;

import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.http.ICommandExecutor;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;

public class HistoryCommandExecutor implements ICommandExecutor<BlConversationKey, BlConversation> {

	private final IConversationManager conversationManager;

	public HistoryCommandExecutor(IConversationManager conversationManager) {
		this.conversationManager = conversationManager;
	}

	@Override
	public BlConversation execute(BlConversationKey key) {
		return conversationManager.getConversation(key);
	}
}
