package com.vacationary.backend.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.conversation.IProcessedEventManager;
import com.vacationary.backend.core.interfaces.http.IErrorHandler;
import com.vacationary.backend.core.interfaces.http.IHttpResponseWriter;
import com.vacationary.backend.core.interfaces.http.IRouteHandler;
import com.vacationary.backend.http.factories.HistoryFeatureFactory;
import com.vacationary.backend.http.factories.TurnsFeatureFactory;
import com.vacationary.backend.http.spring.HttpErrorHandler;
import com.vacationary.backend.http.spring.HttpResponseWriter;

@Configuration
public class HttpConfiguration {

	@Bean
	IHttpResponseWriter httpResponseWriter(ObjectMapper objectMapper) {
		return new HttpResponseWriter(objectMapper);
	}

	@Bean
	IErrorHandler httpErrorHandler(IHttpResponseWriter httpResponseWriter) {
		return new HttpErrorHandler(httpResponseWriter);
	}

	@Bean
	TurnsFeatureFactory turnsFeatureFactory(
			ObjectMapper objectMapper,
			IProcessedEventManager processedEventManager,
			IConversationManager conversationManager,
			IAssistantManager assistantManager,
			IHttpResponseWriter httpResponseWriter,
			IErrorHandler httpErrorHandler) {
		return new TurnsFeatureFactory(
				objectMapper,
				processedEventManager,
				conversationManager,
				assistantManager,
				httpResponseWriter,
				httpErrorHandler);
	}

	@Bean
	HistoryFeatureFactory historyFeatureFactory(
			IConversationManager conversationManager,
			IHttpResponseWriter httpResponseWriter,
			IErrorHandler httpErrorHandler) {
		return new HistoryFeatureFactory(conversationManager, httpResponseWriter, httpErrorHandler);
	}

	@Bean("turnsRouteHandler")
	IRouteHandler turnsRouteHandler(TurnsFeatureFactory turnsFeatureFactory) {
		return turnsFeatureFactory.create();
	}

	@Bean("historyRouteHandler")
	IRouteHandler historyRouteHandler(HistoryFeatureFactory historyFeatureFactory) {
		return historyFeatureFactory.create();
	}
}
