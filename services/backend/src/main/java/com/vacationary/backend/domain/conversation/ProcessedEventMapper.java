package com.vacationary.backend.domain.conversation;

import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlProcessedEvent;
import com.vacationary.backend.dtos.conversation.DbProcessedEvent;

public class ProcessedEventMapper {

	public DbProcessedEvent toDb(BlProcessedEvent event) {
		BlAssistantResponse outcome = event.outcome();
		BlEventKey key = event.key();
		return new DbProcessedEvent(
				key.channel(),
				key.eventId(),
				outcome.kind(),
				outcome.text(),
				ConversationMapper.toDbOptions(outcome.recommendations()));
	}

	public BlProcessedEvent toBl(DbProcessedEvent row) {
		BlAssistantResponse outcome = new BlAssistantResponse(
				row.kind(),
				row.text(),
				ConversationMapper.toBlOptions(row.recommendations()));
		return new BlProcessedEvent(new BlEventKey(row.channel(), row.eventId()), outcome);
	}
}
