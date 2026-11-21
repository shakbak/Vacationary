package com.vacationary.backend.http.features.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletResponse;

import com.vacationary.backend.constants.HttpStatuses;
import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;
import com.vacationary.backend.core.interfaces.http.IResponseHandler;
import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlMessage;
import com.vacationary.backend.dtos.http.ApiConversationResponse;
import com.vacationary.backend.dtos.http.ApiMessage;
import com.vacationary.backend.dtos.http.ApiStayOption;
import com.vacationary.backend.dtos.recommendation.BlStayOption;

public class HistoryResponseHandler implements IResponseHandler<BlConversation> {

	private final IHttpResponseWriter responseWriter;

	public HistoryResponseHandler(IHttpResponseWriter responseWriter) {
		this.responseWriter = responseWriter;
	}

	@Override
	public void handle(BlConversation conversation, HttpServletResponse response) {
		List<ApiMessage> messages = new ArrayList<>();
		for (BlMessage message : conversation.messages()) {
			messages.add(new ApiMessage(
					message.role().name().toLowerCase(Locale.ROOT),
					message.text(),
					toApi(message.recommendations()),
					message.outcomeKind() == null ? null : message.outcomeKind().name().toLowerCase(Locale.ROOT)));
		}
		responseWriter.writeJson(response, HttpStatuses.OK, new ApiConversationResponse(messages));
	}

	private static List<ApiStayOption> toApi(List<BlStayOption> options) {
		List<ApiStayOption> copied = new ArrayList<>();
		for (BlStayOption option : options) {
			copied.add(ApiStayOption.fromBl(option));
		}
		return copied;
	}
}
