package com.vacationary.backend.http.features.history;

import com.vacationary.backend.core.interfaces.http.IRequestMapper;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.http.ApiHistoryRequest;
import com.vacationary.backend.http.spring.HttpChannels;

public class HistoryRequestMapper implements IRequestMapper<ApiHistoryRequest, BlConversationKey> {

	@Override
	public BlConversationKey mapApiToBl(ApiHistoryRequest request) {
		return new BlConversationKey(
				HttpChannels.parse(request.channel()),
				HttpChannels.trimToNull(request.externalUserId()));
	}
}
