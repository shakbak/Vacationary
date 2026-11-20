package com.vacationary.backend.persistence.jdbc.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.conversation.DbConversation;
import com.vacationary.backend.dtos.conversation.DbMessage;
import com.vacationary.backend.dtos.conversation.DbStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;

class JdbcConversationStoreTest {

	private final DataSource dataSource = H2TestDataSource.create();
	private final JdbcConversationStore store = new JdbcConversationStore(dataSource);
	private final JdbcTemplate jdbc = new JdbcTemplate(dataSource);

	@Test
	void missingKeyIsEmpty() {
		assertTrue(store.find(null).isEmpty());
		assertTrue(store.find(new BlConversationKey(null, "alice")).isEmpty());
		assertTrue(store.find(new BlConversationKey(ConversationChannel.WEB, null)).isEmpty());
		assertTrue(store.find(new BlConversationKey(ConversationChannel.WEB, "nobody")).isEmpty());
	}

	@Test
	void saveThenFindReturnsTheSameKeyOnly() {
		DbConversation row = new DbConversation(
				ConversationChannel.WEB,
				"alice",
				List.of(new DbMessage(ChatRole.USER, "Rome", Instant.now(), null, List.of())));
		store.save(row);
		assertEquals("Rome", store.find(new BlConversationKey(ConversationChannel.WEB, "alice")).orElseThrow()
				.messages().getFirst().text());
		assertTrue(store.find(new BlConversationKey(ConversationChannel.WHATSAPP, "alice")).isEmpty());
		assertTrue(store.find(new BlConversationKey(ConversationChannel.WEB, "bob")).isEmpty());
	}

	@Test
	void saveReplacesMessagesAndStoresStayOptions() {
		store.save(new DbConversation(
				ConversationChannel.WEB,
				"alice",
				List.of(new DbMessage(ChatRole.USER, "old", Instant.now(), null, List.of()))));
		DbStayOption option = new DbStayOption(
				"Alpha", StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/Alpha");
		store.save(new DbConversation(
				ConversationChannel.WEB,
				"alice",
				List.of(
						new DbMessage(ChatRole.USER, "Rome", Instant.now(), null, List.of()),
						new DbMessage(
								ChatRole.ASSISTANT,
								"Five places.",
								Instant.now(),
								AssistantResponseKind.RECOMMEND,
								List.of(option)))));
		DbConversation found = store.find(new BlConversationKey(ConversationChannel.WEB, "alice")).orElseThrow();
		assertEquals(2, found.messages().size());
		assertEquals("Rome", found.messages().getFirst().text());
		assertEquals("Alpha", found.messages().get(1).recommendations().getFirst().name());
		assertEquals(StayType.HOTEL, found.messages().get(1).recommendations().getFirst().stayType());
	}

	@Test
	void optionsStayOnTheMessageTheyWereSavedWithAndKeepOrder() {
		store.save(new DbConversation(
				ConversationChannel.WEB,
				"alice",
				List.of(
						new DbMessage(
								ChatRole.USER,
								"Rome",
								Instant.now(),
								null,
								List.of(option("UserSide"))),
						new DbMessage(
								ChatRole.ASSISTANT,
								"Five places.",
								Instant.now(),
								AssistantResponseKind.RECOMMEND,
								List.of(option("A"), option("B"), option("C"))))));
		DbConversation found = store.find(new BlConversationKey(ConversationChannel.WEB, "alice")).orElseThrow();
		assertEquals(List.of("UserSide"), names(found.messages().get(0).recommendations()));
		assertEquals(List.of("A", "B", "C"), names(found.messages().get(1).recommendations()));
	}

	@Test
	void replaceDeletesOrphanStayOptions() {
		store.save(new DbConversation(
				ConversationChannel.WEB,
				"alice",
				List.of(new DbMessage(
						ChatRole.ASSISTANT,
						"Five places.",
						Instant.now(),
						AssistantResponseKind.RECOMMEND,
						List.of(option("A"), option("B"))))));
		store.save(new DbConversation(
				ConversationChannel.WEB,
				"alice",
				List.of(new DbMessage(ChatRole.USER, "Rome", Instant.now(), null, List.of()))));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM stay_options", Integer.class));
		assertTrue(store.find(new BlConversationKey(ConversationChannel.WEB, "alice")).orElseThrow()
				.messages().getFirst().recommendations().isEmpty());
	}

	private static DbStayOption option(String name) {
		return new DbStayOption(
				name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}

	private static List<String> names(List<DbStayOption> options) {
		return options.stream().map(DbStayOption::name).toList();
	}
}
