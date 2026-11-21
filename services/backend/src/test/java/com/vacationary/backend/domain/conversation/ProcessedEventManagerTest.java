package com.vacationary.backend.domain.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlProcessedEvent;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcProcessedEventStore;

class ProcessedEventManagerTest {

	private final ProcessedEventManager manager = new ProcessedEventManager(
			new JdbcProcessedEventStore(H2TestDataSource.create()),
			new ProcessedEventMapper());

	@Test
	void missingEventIsEmpty() {
		assertTrue(manager.getEvent(eventKey(ConversationChannel.WEB, "missing")).isEmpty());
	}

	@Test
	void invalidGetEventOrSaveThrows() {
		assertThrows(BlValidationError.class, () -> manager.getEvent(null));
		assertThrows(BlValidationError.class, () -> manager.getEvent(eventKey(null, "e1")));
		assertThrows(BlValidationError.class, () -> manager.getEvent(eventKey(ConversationChannel.WEB, "  ")));
		assertThrows(BlValidationError.class, () -> manager.save(null));
		assertThrows(BlValidationError.class, () -> manager.save(
				new BlProcessedEvent(eventKey(ConversationChannel.WEB, "e1"), null)));
		assertThrows(BlValidationError.class, () -> manager.save(new BlProcessedEvent(
				eventKey(ConversationChannel.WEB, "e1"),
				new BlAssistantResponse(null, "Where?", List.of()))));
		assertThrows(BlValidationError.class, () -> manager.save(new BlProcessedEvent(
				eventKey(ConversationChannel.WEB, "e1"),
				BlAssistantResponse.clarify("  "))));
	}

	@Test
	void saveThenGetEventReturnsTheOutcomeNotATurnResult() {
		BlAssistantResponse outcome = BlAssistantResponse.clarify("Where would you like to stay?");
		manager.save(new BlProcessedEvent(eventKey(ConversationChannel.TELEGRAM, "evt-1"), outcome));
		Optional<BlProcessedEvent> found = manager.getEvent(eventKey(ConversationChannel.TELEGRAM, "evt-1"));
		assertTrue(found.isPresent());
		assertEquals(AssistantResponseKind.CLARIFY, found.get().outcome().kind());
		assertEquals("Where would you like to stay?", found.get().outcome().text());
		assertTrue(manager.getEvent(eventKey(ConversationChannel.WEB, "evt-1")).isEmpty());
	}

	@Test
	void saveCopiesRecommendationOptions() {
		BlStayOption option = new BlStayOption(
				"Alpha", StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/Alpha");
		BlAssistantResponse outcome = BlAssistantResponse.recommend("Five places.", List.of(option));
		manager.save(new BlProcessedEvent(eventKey(ConversationChannel.WEB, "rome-1"), outcome));
		BlProcessedEvent found = manager.getEvent(eventKey(ConversationChannel.WEB, "rome-1")).orElseThrow();
		assertEquals(1, found.outcome().recommendations().size());
		assertEquals("Alpha", found.outcome().recommendations().getFirst().name());
	}

	private static BlEventKey eventKey(ConversationChannel channel, String eventId) {
		return new BlEventKey(channel, eventId);
	}
}
