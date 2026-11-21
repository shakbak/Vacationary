package com.vacationary.backend.persistence.jdbc.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.conversation.DbProcessedEvent;
import com.vacationary.backend.dtos.conversation.DbStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;

class JdbcProcessedEventStoreTest {

	private final DataSource dataSource = H2TestDataSource.create();
	private final JdbcProcessedEventStore store = new JdbcProcessedEventStore(dataSource);
	private final JdbcTemplate jdbc = new JdbcTemplate(dataSource);

	@Test
	void missingEventIsEmpty() {
		assertTrue(store.find(null).isEmpty());
		assertTrue(store.find(new BlEventKey(null, "evt-1")).isEmpty());
		assertTrue(store.find(new BlEventKey(ConversationChannel.WEB, null)).isEmpty());
		assertTrue(store.find(new BlEventKey(ConversationChannel.WEB, "missing")).isEmpty());
	}

	@Test
	void saveThenFindIsScopedToChannelAndEventId() {
		store.save(new DbProcessedEvent(
				ConversationChannel.WEB, "evt-1", AssistantResponseKind.CLARIFY, "Where?", List.of()));
		assertEquals("Where?", store.find(new BlEventKey(ConversationChannel.WEB, "evt-1")).orElseThrow().text());
		assertTrue(store.find(new BlEventKey(ConversationChannel.TELEGRAM, "evt-1")).isEmpty());
		assertTrue(store.find(new BlEventKey(ConversationChannel.WEB, "evt-2")).isEmpty());
	}

	@Test
	void saveReplacesOutcomeAndStoresStayOptions() {
		store.save(new DbProcessedEvent(
				ConversationChannel.WEB, "evt-1", AssistantResponseKind.CLARIFY, "Where?", List.of()));
		DbStayOption option = new DbStayOption(
				"Alpha", StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/Alpha");
		store.save(new DbProcessedEvent(
				ConversationChannel.WEB,
				"evt-1",
				AssistantResponseKind.RECOMMEND,
				"Five places.",
				List.of(option)));
		DbProcessedEvent found = store.find(new BlEventKey(ConversationChannel.WEB, "evt-1")).orElseThrow();
		assertEquals(AssistantResponseKind.RECOMMEND, found.kind());
		assertEquals("Five places.", found.text());
		assertEquals("Alpha", found.recommendations().getFirst().name());
	}

	@Test
	void optionsKeepInsertOrder() {
		store.save(new DbProcessedEvent(
				ConversationChannel.WEB,
				"evt-1",
				AssistantResponseKind.RECOMMEND,
				"Five places.",
				List.of(option("A"), option("B"), option("C"))));
		assertEquals(
				List.of("A", "B", "C"),
				store.find(new BlEventKey(ConversationChannel.WEB, "evt-1")).orElseThrow()
						.recommendations().stream().map(DbStayOption::name).toList());
	}

	@Test
	void replaceDeletesOrphanStayOptions() {
		store.save(new DbProcessedEvent(
				ConversationChannel.WEB,
				"evt-1",
				AssistantResponseKind.RECOMMEND,
				"Five places.",
				List.of(option("A"), option("B"))));
		store.save(new DbProcessedEvent(
				ConversationChannel.WEB, "evt-1", AssistantResponseKind.CLARIFY, "Where?", List.of()));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM stay_options", Integer.class));
	}

	private static DbStayOption option(String name) {
		return new DbStayOption(
				name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
