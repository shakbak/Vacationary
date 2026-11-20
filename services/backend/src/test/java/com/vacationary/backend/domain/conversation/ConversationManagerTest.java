package com.vacationary.backend.domain.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlAssistantException;
import com.vacationary.backend.errors.bl.BlValidationError;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcConversationStore;

class ConversationManagerTest {

	private final FakeAssistantManager assistant = new FakeAssistantManager();
	private final ConversationManager manager = new ConversationManager(
			new JdbcConversationStore(H2TestDataSource.create()),
			new ConversationMapper(),
			assistant);

	@Test
	void nullKeyOrBlankTextThrow() {
		assertThrows(BlValidationError.class, () -> manager.handleTurn(null, "Rome"));
		assertThrows(BlValidationError.class, () -> manager.handleTurn(
				new BlConversationKey(null, "u1"), "Rome"));
		assertThrows(BlValidationError.class, () -> manager.handleTurn(
				new BlConversationKey(ConversationChannel.WEB, "  "), "Rome"));
		assertThrows(BlValidationError.class, () -> manager.handleTurn(
				new BlConversationKey(ConversationChannel.WEB, "u1"), "  "));
	}

	@Test
	void unknownConversationIsEmptyNotAnError() {
		BlConversation conversation = manager.getConversation(
				new BlConversationKey(ConversationChannel.WEB, "nobody"));
		assertTrue(conversation.messages().isEmpty());
		assertEquals("nobody", conversation.key().externalUserId());
	}

	@Test
	void turnAppendsUserAndAssistantAndReturnsOutcome() {
		assistant.replyWith(BlAssistantResponse.clarify("Where would you like to stay?"));
		BlAssistantResponse response = manager.handleTurn(key(ConversationChannel.WEB, "alice"), "I need a place");
		assertEquals(AssistantResponseKind.CLARIFY, response.kind());
		BlConversation conversation = manager.getConversation(key(ConversationChannel.WEB, "alice"));
		assertEquals(2, conversation.messages().size());
		assertEquals(ChatRole.USER, conversation.messages().get(0).role());
		assertEquals("I need a place", conversation.messages().get(0).text());
		assertEquals(ChatRole.ASSISTANT, conversation.messages().get(1).role());
		assertEquals(AssistantResponseKind.CLARIFY, conversation.messages().get(1).outcomeKind());
		assertEquals(1, assistant.capturedRequests().size());
		assertEquals("I need a place", assistant.capturedRequests().getFirst().latestUserText());
	}

	@Test
	void followUpPassesThisConversationOnlyIncludingPriorTurns() {
		assistant.replyWith(BlAssistantResponse.clarify("Where would you like to stay?"));
		manager.handleTurn(key(ConversationChannel.WEB, "alice"), "hotels in Lisbon");
		List<BlStayOption> five = List.of(
				option("A"), option("B"), option("C"), option("D"), option("E"));
		assistant.replyWith(BlAssistantResponse.recommend("Here are five cheaper hotels in Lisbon.", five));
		BlAssistantResponse response = manager.handleTurn(key(ConversationChannel.WEB, "alice"), "cheaper");
		assertEquals(AssistantResponseKind.RECOMMEND, response.kind());
		assertEquals(5, response.recommendations().size());
		List<BlChatMessage> messages = assistant.capturedRequests().get(1).messages();
		assertEquals(3, messages.size());
		assertEquals("hotels in Lisbon", messages.get(0).text());
		assertEquals(ChatRole.ASSISTANT, messages.get(1).role());
		assertEquals("cheaper", messages.get(2).text());
		assertEquals("cheaper", assistant.capturedRequests().get(1).latestUserText());
	}

	@Test
	void usersAreIsolatedByChannelAndExternalId() {
		assistant.replyWith(BlAssistantResponse.clarify("Where, Alice?"));
		manager.handleTurn(key(ConversationChannel.WEB, "alice"), "hello");
		assistant.replyWith(BlAssistantResponse.clarify("Where, Bob?"));
		manager.handleTurn(key(ConversationChannel.WEB, "bob"), "hi");
		assistant.replyWith(BlAssistantResponse.clarify("Where on WhatsApp?"));
		manager.handleTurn(key(ConversationChannel.WHATSAPP, "alice"), "yo");

		assertEquals("Where, Alice?", manager.getConversation(key(ConversationChannel.WEB, "alice")).messages().get(1).text());
		assertEquals("Where, Bob?", manager.getConversation(key(ConversationChannel.WEB, "bob")).messages().get(1).text());
		assertEquals("Where on WhatsApp?", manager.getConversation(
				key(ConversationChannel.WHATSAPP, "alice")).messages().get(1).text());
		assertTrue(manager.getConversation(key(ConversationChannel.WHATSAPP, "bob")).messages().isEmpty());
	}

	@Test
	void assistantThrowIsFailNotAHotelList() {
		assistant.failWith(new BlAssistantException("LLM down"));
		BlAssistantResponse response = manager.handleTurn(key(ConversationChannel.WEB, "alice"), "Rome");
		assertEquals(AssistantResponseKind.FAIL, response.kind());
		assertEquals(ConversationManager.ASSISTANT_FAIL, response.text());
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void recommendStoresToolOptionsOnTheAssistantMessage() {
		List<BlStayOption> five = List.of(
				option("A"), option("B"), option("C"), option("D"), option("E"));
		assistant.replyWith(BlAssistantResponse.recommend("Five places in Rome.", five));
		manager.handleTurn(key(ConversationChannel.WEB, "alice"), "Rome");
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
