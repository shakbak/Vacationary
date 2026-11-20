package com.vacationary.backend.bootstrap;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationManager;
import com.vacationary.backend.core.interfaces.conversation.IConversationRepository;
import com.vacationary.backend.core.interfaces.conversation.IProcessedEventManager;
import com.vacationary.backend.core.interfaces.conversation.IProcessedEventRepository;
import com.vacationary.backend.domain.conversation.ConversationManager;
import com.vacationary.backend.domain.conversation.ConversationMapper;
import com.vacationary.backend.domain.conversation.ProcessedEventManager;
import com.vacationary.backend.domain.conversation.ProcessedEventMapper;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcConversationStore;
import com.vacationary.backend.persistence.jdbc.conversation.JdbcProcessedEventStore;

@Configuration
public class ConversationConfiguration {

	@Bean
	ConversationMapper conversationMapper() {
		return new ConversationMapper();
	}

	@Bean
	ProcessedEventMapper processedEventMapper() {
		return new ProcessedEventMapper();
	}

	@Bean
	IConversationRepository conversationRepository(DataSource dataSource) {
		return new JdbcConversationStore(dataSource);
	}

	@Bean
	IProcessedEventRepository processedEventRepository(DataSource dataSource) {
		return new JdbcProcessedEventStore(dataSource);
	}

	@Bean
	IConversationManager conversationManager(
			IConversationRepository conversationRepository,
			ConversationMapper conversationMapper,
			IAssistantManager assistantManager) {
		return new ConversationManager(
				conversationRepository,
				conversationMapper,
				assistantManager);
	}

	@Bean
	IProcessedEventManager processedEventManager(
			IProcessedEventRepository processedEventRepository,
			ProcessedEventMapper processedEventMapper) {
		return new ProcessedEventManager(processedEventRepository, processedEventMapper);
	}
}
