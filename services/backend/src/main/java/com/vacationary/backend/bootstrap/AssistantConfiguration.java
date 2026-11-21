package com.vacationary.backend.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.core.interfaces.assistant.ILlmClient;
import com.vacationary.backend.core.interfaces.assistant.ILlmManager;
import com.vacationary.backend.core.interfaces.assistant.ILlmPromptBuilder;
import com.vacationary.backend.core.interfaces.assistant.IOpenAiChatMapper;
import com.vacationary.backend.core.interfaces.recommendation.IStayManager;
import com.vacationary.backend.domain.assistant.AssistantManager;
import com.vacationary.backend.domain.assistant.AssistantResponseMapper;
import com.vacationary.backend.domain.assistant.LlmManager;
import com.vacationary.backend.domain.assistant.LlmPromptBuilder;
import com.vacationary.backend.domain.assistant.StaySearchParser;
import com.vacationary.backend.infra.llm.HttpLlmClient;
import com.vacationary.backend.infra.llm.LlmApiKey;
import com.vacationary.backend.infra.llm.OpenAiChatMapper;

@Configuration
public class AssistantConfiguration {

	@Bean
	IOpenAiChatMapper openAiChatMapper(ObjectMapper objectMapper) {
		return new OpenAiChatMapper(objectMapper);
	}

	@Bean
	ILlmClient llmClient(
			@Value("${vacationary.llm.base-url}") String baseUrl,
			@Value("${vacationary.llm.model}") String model,
			@Value("${vacationary.llm.api-key:}") String apiKey,
			IOpenAiChatMapper openAiChatMapper) {
		return new HttpLlmClient(baseUrl, model, LlmApiKey.resolve(apiKey), openAiChatMapper);
	}

	@Bean
	ILlmPromptBuilder llmPromptBuilder(ObjectMapper objectMapper) {
		return new LlmPromptBuilder(objectMapper);
	}

	@Bean
	ILlmManager llmManager(ILlmPromptBuilder llmPromptBuilder, ILlmClient llmClient) {
		return new LlmManager(llmPromptBuilder, llmClient);
	}

	@Bean
	AssistantResponseMapper assistantResponseMapper() {
		return new AssistantResponseMapper();
	}

	@Bean
	StaySearchParser staySearchParser(ObjectMapper objectMapper) {
		return new StaySearchParser(objectMapper);
	}

	@Bean
	IAssistantManager assistantManager(
			ILlmManager llmManager,
			StaySearchParser staySearchParser,
			IStayManager stayManager,
			AssistantResponseMapper assistantResponseMapper) {
		return new AssistantManager(
				llmManager,
				staySearchParser,
				stayManager,
				assistantResponseMapper);
	}
}
