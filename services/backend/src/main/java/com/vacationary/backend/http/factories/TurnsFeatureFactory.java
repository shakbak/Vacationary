package com.vacationary.backend.http.factories;

import tools.jackson.databind.ObjectMapper;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.conversation.IProcessedEventManager;
import com.vacationary.backend.core.interfaces.http.IErrorHandler;
import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;
import com.vacationary.backend.core.interfaces.http.IRouteHandler;
import com.vacationary.backend.http.features.turns.TurnsCommandExecutor;
import com.vacationary.backend.http.features.turns.TurnsRequestMapper;
import com.vacationary.backend.http.features.turns.TurnsRequestReader;
import com.vacationary.backend.http.features.turns.TurnsResponseHandler;
import com.vacationary.backend.http.features.turns.TurnsSchemaValidator;
import com.vacationary.backend.http.spring.RouteHandler;

public class TurnsFeatureFactory {

	private final ObjectMapper objectMapper;
	private final IProcessedEventManager processedEventManager;
	private final IConversationManager conversationManager;
	private final IAssistantManager assistantManager;
	private final IHttpResponseWriter responseWriter;
	private final IErrorHandler errorHandler;

	public TurnsFeatureFactory(
			ObjectMapper objectMapper,
			IProcessedEventManager processedEventManager,
			IConversationManager conversationManager,
			IAssistantManager assistantManager,
			IHttpResponseWriter responseWriter,
			IErrorHandler errorHandler) {
		this.objectMapper = objectMapper;
		this.processedEventManager = processedEventManager;
		this.conversationManager = conversationManager;
		this.assistantManager = assistantManager;
		this.responseWriter = responseWriter;
		this.errorHandler = errorHandler;
	}

	public IRouteHandler create() {
		return new RouteHandler<>(
				new TurnsRequestReader(objectMapper),
				new TurnsSchemaValidator(),
				new TurnsRequestMapper(),
				new TurnsCommandExecutor(processedEventManager, conversationManager, assistantManager),
				new TurnsResponseHandler(responseWriter),
				errorHandler);
	}
}
