package com.vacationary.backend.core.interfaces.conversation;

import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;

public interface IConversationManager {

	BlAssistantResponse handleTurn(BlConversationKey key, String text);

	BlConversation getConversation(BlConversationKey key);
}
