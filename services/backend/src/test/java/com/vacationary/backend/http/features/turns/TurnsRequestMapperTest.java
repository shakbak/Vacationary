package com.vacationary.backend.http.features.turns;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.conversation.BlTurnCommand;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.http.ApiTurnRequest;

class TurnsRequestMapperTest {

	private final TurnsRequestMapper mapper = new TurnsRequestMapper();

	@Test
	void mapsLowercasedChannelAndTrimsFields() {
		BlTurnCommand command = mapper.mapApiToBl(new ApiTurnRequest(" WEB ", " alice ", " Rome ", " evt-1 "));
		assertEquals(ConversationChannel.WEB, command.conversation().channel());
		assertEquals("alice", command.conversation().externalUserId());
		assertEquals("Rome", command.text());
		assertEquals("evt-1", command.eventId());
	}

	@Test
	void mapsTelegram() {
		BlTurnCommand command = mapper.mapApiToBl(new ApiTurnRequest("telegram", "u1", "hi", "e1"));
		assertEquals(ConversationChannel.TELEGRAM, command.conversation().channel());
	}
}
