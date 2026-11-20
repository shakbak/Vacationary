package com.vacationary.backend.core.interfaces.conversation;

import java.util.Optional;

import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.DbProcessedEvent;

public interface IProcessedEventRepository {

	Optional<DbProcessedEvent> find(BlEventKey key);

	void save(DbProcessedEvent event);
}
