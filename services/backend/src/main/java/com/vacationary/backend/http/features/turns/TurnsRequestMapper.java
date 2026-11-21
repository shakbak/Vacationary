package com.vacationary.backend.http.features.turns;

import com.vacationary.backend.core.interfaces.http.IRequestMapper;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.BlTurnCommand;
import com.vacationary.backend.dtos.http.ApiTurnRequest;
import com.vacationary.backend.http.spring.HttpChannels;

public class TurnsRequestMapper implements IRequestMapper<ApiTurnRequest, BlTurnCommand> {

	@Override
	public BlTurnCommand mapApiToBl(ApiTurnRequest request) {
		return new BlTurnCommand(
				new BlConversationKey(
						HttpChannels.parse(request.channel()),
						HttpChannels.trimToNull(request.externalUserId())),
				HttpChannels.trimToNull(request.text()),
				HttpChannels.trimToNull(request.eventId()));
	}
}
