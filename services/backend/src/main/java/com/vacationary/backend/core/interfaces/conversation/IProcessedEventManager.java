package com.vacationary.backend.core.interfaces.conversation;

import java.util.Optional;

import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlProcessedEvent;

public interface IProcessedEventManager {

	Optional<BlProcessedEvent> find(BlEventKey key);

	void save(BlProcessedEvent event);
}
