package com.vacationary.backend.http.factories;

import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.http.IErrorHandler;
import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;
import com.vacationary.backend.core.interfaces.http.IRouteHandler;
import com.vacationary.backend.http.features.history.HistoryCommandExecutor;
import com.vacationary.backend.http.features.history.HistoryRequestMapper;
import com.vacationary.backend.http.features.history.HistoryRequestReader;
import com.vacationary.backend.http.features.history.HistoryResponseHandler;
import com.vacationary.backend.http.features.history.HistorySchemaValidator;
import com.vacationary.backend.http.spring.RouteHandler;

public class HistoryFeatureFactory {

	private final IConversationManager conversationManager;
	private final IHttpResponseWriter responseWriter;
	private final IErrorHandler errorHandler;

	public HistoryFeatureFactory(
			IConversationManager conversationManager,
			IHttpResponseWriter responseWriter,
			IErrorHandler errorHandler) {
		this.conversationManager = conversationManager;
		this.responseWriter = responseWriter;
		this.errorHandler = errorHandler;
	}

	public IRouteHandler create() {
		return new RouteHandler<>(
				new HistoryRequestReader(),
				new HistorySchemaValidator(),
				new HistoryRequestMapper(),
				new HistoryCommandExecutor(conversationManager),
				new HistoryResponseHandler(responseWriter),
				errorHandler);
	}
}
