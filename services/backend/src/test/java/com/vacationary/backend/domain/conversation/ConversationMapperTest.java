package com.vacationary.backend.domain.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.BlMessage;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.conversation.DbConversation;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;

class ConversationMapperTest {

	private final ConversationMapper mapper = new ConversationMapper();

	@Test
	void roundTripsMessagesAndCopiedOptions() {
		BlStayOption option = new BlStayOption(
				"Alpha", StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/Alpha");
		BlConversation original = new BlConversation(
				new BlConversationKey(ConversationChannel.WEB, "alice"),
				List.of(
						new BlMessage(ChatRole.USER, "Rome", Instant.parse("2026-11-20T12:00:00Z"), null, List.of()),
						new BlMessage(
								ChatRole.ASSISTANT,
								"Five places.",
								Instant.parse("2026-11-20T12:00:01Z"),
								AssistantResponseKind.RECOMMEND,
								List.of(option))));
		DbConversation db = mapper.toDb(original);
		BlConversation restored = mapper.toBl(db);
		assertEquals(ConversationChannel.WEB, restored.key().channel());
		assertEquals("alice", restored.key().externalUserId());
		assertEquals(2, restored.messages().size());
		assertEquals("Rome", restored.messages().get(0).text());
		assertNull(restored.messages().get(0).outcomeKind());
		assertEquals("Alpha", restored.messages().get(1).recommendations().getFirst().name());
		assertEquals(AssistantResponseKind.RECOMMEND, restored.messages().get(1).outcomeKind());
	}
}
