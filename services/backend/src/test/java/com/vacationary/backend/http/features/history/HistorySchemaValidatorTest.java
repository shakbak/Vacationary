package com.vacationary.backend.http.features.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.http.ApiHistoryRequest;
import com.vacationary.backend.errors.bl.BlValidationError;

class HistorySchemaValidatorTest {

	private final HistorySchemaValidator validator = new HistorySchemaValidator();
	private final HistoryRequestMapper mapper = new HistoryRequestMapper();
	private final HistoryCommandExecutor executor = new HistoryCommandExecutor(new EmptyConversationManager());

	@Test
	void validPathPassesAndMaps() {
		assertDoesNotThrow(() -> validator.validate(new ApiHistoryRequest("web", "alice")));
		BlConversationKey key = mapper.mapApiToBl(new ApiHistoryRequest(" TELEGRAM ", " alice "));
		assertEquals(ConversationChannel.TELEGRAM, key.channel());
		assertEquals("alice", key.externalUserId());
	}

	@Test
	void unknownChannelFails() {
		assertThrows(BlValidationError.class, () -> validator.validate(new ApiHistoryRequest("slack", "alice")));
	}

	@Test
	void unknownConversationIsEmpty() {
		BlConversation conversation = executor.execute(new BlConversationKey(ConversationChannel.WEB, "nobody"));
		assertTrue(conversation.messages().isEmpty());
	}

	private static final class EmptyConversationManager implements IConversationManager {

		@Override
		public void saveTurn(BlConversationKey key, String text, BlAssistantResponse response) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BlConversation getConversation(BlConversationKey key) {
			return BlConversation.empty(key);
		}
	}
}
