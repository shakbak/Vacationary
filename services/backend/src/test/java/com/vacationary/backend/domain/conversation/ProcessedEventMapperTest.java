package com.vacationary.backend.domain.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlProcessedEvent;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.conversation.DbProcessedEvent;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;

class ProcessedEventMapperTest {

	private final ProcessedEventMapper mapper = new ProcessedEventMapper();

	@Test
	void roundTripsOutcomeWithoutADuplicateFlag() {
		BlStayOption option = new BlStayOption(
				"Alpha", StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/Alpha");
		BlProcessedEvent original = new BlProcessedEvent(
				new BlEventKey(ConversationChannel.WEB, "evt-1"),
				BlAssistantResponse.recommend("Five places.", List.of(option)));
		DbProcessedEvent db = mapper.toDb(original);
		assertEquals("Alpha", db.recommendations().getFirst().name());
		BlProcessedEvent restored = mapper.toBl(db);
		assertEquals(ConversationChannel.WEB, restored.key().channel());
		assertEquals("evt-1", restored.key().eventId());
		assertEquals(AssistantResponseKind.RECOMMEND, restored.outcome().kind());
		assertEquals("Five places.", restored.outcome().text());
		assertEquals("Alpha", restored.outcome().recommendations().getFirst().name());
	}
}
