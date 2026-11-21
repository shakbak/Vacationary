package com.vacationary.backend.http.features.turns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.domain.conversation.ConversationManager;
import com.vacationary.backend.domain.conversation.ConversationMapper;
import com.vacationary.backend.domain.conversation.ProcessedEventManager;
import com.vacationary.backend.domain.conversation.ProcessedEventMapper;
import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlTurnCommand;
import com.vacationary.backend.dtos.conversation.BlTurnResult;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlAssistantException;
import com.vacationary.backend.persistence.jdbc.H2TestDataSource;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcConversationStore;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcProcessedEventStore;

class TurnsCommandExecutorTest {

	private final DataSource dataSource = H2TestDataSource.create();
	private final ConversationManager conversations = new ConversationManager(
			new JdbcConversationStore(dataSource),
			new ConversationMapper());
	private final ProcessedEventManager events = new ProcessedEventManager(
			new JdbcProcessedEventStore(dataSource),
			new ProcessedEventMapper());
	private final FakeAssistantManager assistant = new FakeAssistantManager();
	private final TurnsCommandExecutor executor = new TurnsCommandExecutor(events, conversations, assistant);

	@Test
	void firstTurnRunsAssistantAndSavesEvent() {
		assistant.replyWith(BlAssistantResponse.clarify("Where would you like to stay?"));
		BlTurnResult result = executor.execute(command(ConversationChannel.WEB, "alice", "Rome", "evt-1"));
		assertFalse(result.duplicate());
		assertEquals(AssistantResponseKind.CLARIFY, result.response().kind());
		assertEquals(1, assistant.captured.size());
		assertEquals("Rome", assistant.captured.getFirst().latestUserText());
		assertTrue(events.getEvent(new BlEventKey(ConversationChannel.WEB, "evt-1")).isPresent());
		BlConversation stored = conversations.getConversation(key(ConversationChannel.WEB, "alice"));
		assertEquals(2, stored.messages().size());
	}

	@Test
	void duplicateEventDoesNotRunAssistantAgain() {
		assistant.replyWith(BlAssistantResponse.clarify("Where would you like to stay?"));
		BlTurnCommand command = command(ConversationChannel.WEB, "alice", "Rome", "evt-1");
		executor.execute(command);
		assistant.replyWith(BlAssistantResponse.clarify("should not be used"));
		BlTurnResult duplicate = executor.execute(command);
		assertTrue(duplicate.duplicate());
		assertEquals("Where would you like to stay?", duplicate.response().text());
		assertEquals(1, assistant.captured.size());
	}

	@Test
	void sameEventIdOnAnotherChannelIsANewTurn() {
		assistant.replyWith(BlAssistantResponse.clarify("web"));
		executor.execute(command(ConversationChannel.WEB, "alice", "Rome", "evt-1"));
		assistant.replyWith(BlAssistantResponse.clarify("whatsapp"));
		BlTurnResult result = executor.execute(command(ConversationChannel.WHATSAPP, "alice", "Rome", "evt-1"));
		assertFalse(result.duplicate());
		assertEquals("whatsapp", result.response().text());
		assertEquals(2, assistant.captured.size());
	}

	@Test
	void followUpPassesThisConversationOnlyIncludingPriorTurns() {
		assistant.replyWith(BlAssistantResponse.clarify("Where would you like to stay?"));
		executor.execute(command(ConversationChannel.WEB, "alice", "hotels in Lisbon", "evt-1"));
		List<BlStayOption> five = List.of(option("A"), option("B"), option("C"), option("D"), option("E"));
		assistant.replyWith(BlAssistantResponse.recommend("Here are five cheaper hotels in Lisbon.", five));
		BlTurnResult result = executor.execute(command(ConversationChannel.WEB, "alice", "cheaper", "evt-2"));
		assertEquals(AssistantResponseKind.RECOMMEND, result.response().kind());
		List<BlChatMessage> messages = assistant.captured.get(1).messages();
		assertEquals(3, messages.size());
		assertEquals("hotels in Lisbon", messages.get(0).text());
		assertEquals(ChatRole.ASSISTANT, messages.get(1).role());
		assertEquals("cheaper", messages.get(2).text());
		assertEquals("cheaper", assistant.captured.get(1).latestUserText());
	}

	@Test
	void assistantThrowIsFailNotAHotelList() {
		assistant.failWith(new BlAssistantException("LLM down"));
		BlTurnResult result = executor.execute(command(ConversationChannel.WEB, "alice", "Rome", "evt-1"));
		assertEquals(AssistantResponseKind.FAIL, result.response().kind());
		assertEquals(TurnsCommandExecutor.ASSISTANT_FAIL, result.response().text());
		assertTrue(result.response().recommendations().isEmpty());
		assertEquals(TurnsCommandExecutor.ASSISTANT_FAIL,
				conversations.getConversation(key(ConversationChannel.WEB, "alice")).messages().get(1).text());
	}

	private static BlTurnCommand command(
			ConversationChannel channel,
			String user,
			String text,
			String eventId) {
		return new BlTurnCommand(key(channel, user), text, eventId);
	}

	private static BlConversationKey key(ConversationChannel channel, String user) {
		return new BlConversationKey(channel, user);
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}

	private static final class FakeAssistantManager implements IAssistantManager {

		private final List<BlAssistantRequest> captured = new ArrayList<>();
		private BlAssistantResponse next = BlAssistantResponse.clarify("Where would you like to stay?");
		private RuntimeException failure;

		void replyWith(BlAssistantResponse response) {
			this.next = response;
			this.failure = null;
		}

		void failWith(RuntimeException error) {
			this.failure = error;
		}

		@Override
		public BlAssistantResponse respond(BlAssistantRequest request) {
			captured.add(request);
			if (failure != null) {
				throw failure;
			}
			return next;
		}
	}
}
