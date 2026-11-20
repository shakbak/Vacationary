package com.vacationary.backend.core.interfaces.conversation;

import java.util.Optional;

import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.DbConversation;

public interface IConversationRepository {

	Optional<DbConversation> find(BlConversationKey key);

	void save(DbConversation conversation);
}
