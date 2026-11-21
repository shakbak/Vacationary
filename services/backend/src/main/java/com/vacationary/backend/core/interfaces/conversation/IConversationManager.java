package com.vacationary.backend.core.interfaces.conversation;

import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;

public interface IConversationManager {

	BlConversation getConversation(BlConversationKey key);

	void saveTurn(BlConversationKey key, String text, BlAssistantResponse response);
}
