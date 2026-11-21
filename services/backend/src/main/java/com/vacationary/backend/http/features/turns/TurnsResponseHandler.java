package com.vacationary.backend.http.features.turns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletResponse;

import com.vacationary.backend.constants.HttpStatuses;
import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;
import com.vacationary.backend.core.interfaces.http.IResponseHandler;
import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.conversation.BlTurnResult;
import com.vacationary.backend.dtos.http.ApiStayOption;
import com.vacationary.backend.dtos.http.ApiTurnResponse;
import com.vacationary.backend.dtos.recommendation.BlStayOption;

public class TurnsResponseHandler implements IResponseHandler<BlTurnResult> {

	private final IHttpResponseWriter responseWriter;

	public TurnsResponseHandler(IHttpResponseWriter responseWriter) {
		this.responseWriter = responseWriter;
	}

	@Override
	public void handle(BlTurnResult result, HttpServletResponse response) {
		List<ApiStayOption> recommendations = List.of();
		if (result.response().kind() == AssistantResponseKind.RECOMMEND) {
			recommendations = toApi(result.response().recommendations());
		}
		responseWriter.writeJson(
				response,
				HttpStatuses.OK,
				new ApiTurnResponse(
						outcome(result.response().kind()),
						result.response().text(),
						recommendations,
						result.duplicate()));
	}

	private static String outcome(AssistantResponseKind kind) {
		return kind.name().toLowerCase(Locale.ROOT);
	}

	private static List<ApiStayOption> toApi(List<BlStayOption> options) {
		List<ApiStayOption> copied = new ArrayList<>();
		for (BlStayOption option : options) {
			copied.add(ApiStayOption.fromBl(option));
		}
		return copied;
	}
}
