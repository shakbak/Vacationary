package com.vacationary.backend.domain.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcConversationStore;

class ConversationManagerTest {

	private final ConversationManager manager = new ConversationManager(
			new JdbcConversationStore(H2TestDataSource.create()),
			new ConversationMapper());

	@Test
	void nullKeyOrBlankTextThrow() {
		BlAssistantResponse clarify = BlAssistantResponse.clarify("Where?");
		assertThrows(BlValidationError.class, () -> manager.saveTurn(null, "Rome", clarify));
		assertThrows(BlValidationError.class, () -> manager.saveTurn(
				new BlConversationKey(null, "u1"), "Rome", clarify));
		assertThrows(BlValidationError.class, () -> manager.saveTurn(
				new BlConversationKey(ConversationChannel.WEB, "  "), "Rome", clarify));
		assertThrows(BlValidationError.class, () -> manager.saveTurn(
				new BlConversationKey(ConversationChannel.WEB, "u1"), "  ", clarify));
		assertThrows(BlValidationError.class, () -> manager.saveTurn(
				new BlConversationKey(ConversationChannel.WEB, "u1"), "Rome", null));
		assertThrows(BlValidationError.class, () -> manager.getConversation(null));
	}

	@Test
	void unknownConversationIsEmptyNotAnError() {
		BlConversation conversation = manager.getConversation(
				new BlConversationKey(ConversationChannel.WEB, "nobody"));
		assertTrue(conversation.messages().isEmpty());
		assertEquals("nobody", conversation.key().externalUserId());
	}

	@Test
	void saveTurnAppendsUserAndAssistant() {
		manager.saveTurn(
				key(ConversationChannel.WEB, "alice"),
				"I need a place",
				BlAssistantResponse.clarify("Where would you like to stay?"));
		BlConversation conversation = manager.getConversation(key(ConversationChannel.WEB, "alice"));
		assertEquals(2, conversation.messages().size());
		assertEquals(ChatRole.USER, conversation.messages().get(0).role());
		assertEquals("I need a place", conversation.messages().get(0).text());
		assertEquals(ChatRole.ASSISTANT, conversation.messages().get(1).role());
		assertEquals(AssistantResponseKind.CLARIFY, conversation.messages().get(1).outcomeKind());
		assertEquals("Where would you like to stay?", conversation.messages().get(1).text());
	}

	@Test
	void followUpKeepsPriorTurnsOnThisConversationOnly() {
		manager.saveTurn(
				key(ConversationChannel.WEB, "alice"),
				"hotels in Lisbon",
				BlAssistantResponse.clarify("Where would you like to stay?"));
		List<BlStayOption> five = List.of(option("A"), option("B"), option("C"), option("D"), option("E"));
		manager.saveTurn(
				key(ConversationChannel.WEB, "alice"),
				"cheaper",
				BlAssistantResponse.recommend("Here are five cheaper hotels in Lisbon.", five));
		BlConversation conversation = manager.getConversation(key(ConversationChannel.WEB, "alice"));
		assertEquals(4, conversation.messages().size());
		assertEquals("hotels in Lisbon", conversation.messages().get(0).text());
		assertEquals("cheaper", conversation.messages().get(2).text());
		assertEquals(5, conversation.messages().get(3).recommendations().size());
	}

	@Test
	void usersAreIsolatedByChannelAndExternalId() {
		manager.saveTurn(key(ConversationChannel.WEB, "alice"), "hello", BlAssistantResponse.clarify("Where, Alice?"));
		manager.saveTurn(key(ConversationChannel.WEB, "bob"), "hi", BlAssistantResponse.clarify("Where, Bob?"));
		manager.saveTurn(
				key(ConversationChannel.WHATSAPP, "alice"),
				"yo",
				BlAssistantResponse.clarify("Where on WhatsApp?"));

		assertEquals("Where, Alice?", manager.getConversation(key(ConversationChannel.WEB, "alice")).messages().get(1).text());
		assertEquals("Where, Bob?", manager.getConversation(key(ConversationChannel.WEB, "bob")).messages().get(1).text());
		assertEquals("Where on WhatsApp?", manager.getConversation(
				key(ConversationChannel.WHATSAPP, "alice")).messages().get(1).text());
		assertTrue(manager.getConversation(key(ConversationChannel.WHATSAPP, "bob")).messages().isEmpty());
	}

	@Test
	void recommendStoresToolOptionsOnTheAssistantMessage() {
		List<BlStayOption> five = List.of(option("A"), option("B"), option("C"), option("D"), option("E"));
		manager.saveTurn(
				key(ConversationChannel.WEB, "alice"),
				"Rome",
				BlAssistantResponse.recommend("Five places in Rome.", five));
		BlConversation conversation = manager.getConversation(key(ConversationChannel.WEB, "alice"));
		assertEquals(5, conversation.messages().get(1).recommendations().size());
		assertEquals("A", conversation.messages().get(1).recommendations().getFirst().name());
		assertEquals(AssistantResponseKind.RECOMMEND, conversation.messages().get(1).outcomeKind());
	}

	private static BlConversationKey key(ConversationChannel channel, String user) {
		return new BlConversationKey(channel, user);
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
