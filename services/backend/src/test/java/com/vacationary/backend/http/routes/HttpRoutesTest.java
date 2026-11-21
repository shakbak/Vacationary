package com.vacationary.backend.http.routes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlAssistantException;

@SpringBootTest
@AutoConfigureMockMvc
@Import(HttpRoutesTest.FakeAssistantConfig.class)
class HttpRoutesTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ScriptedAssistantManager assistant;

	@BeforeEach
	void resetAssistant() {
		assistant.reset();
	}

	@Test
	void missingFieldsAre400() throws Exception {
		mockMvc.perform(post("/turns")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void invalidJsonIs400() throws Exception {
		mockMvc.perform(post("/turns")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void turnReturns200AndDuplicateReplaysWithoutASecondAssistantCall() throws Exception {
		assistant.next = BlAssistantResponse.clarify("Where would you like to stay?");
		String body = """
				{"channel":"web","externalUserId":"alice","text":"I need a place","eventId":"evt-1"}
				""";
		mockMvc.perform(post("/turns").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("clarify"))
				.andExpect(jsonPath("$.text").value("Where would you like to stay?"))
				.andExpect(jsonPath("$.duplicate").value(false));
		assistant.next = BlAssistantResponse.clarify("should not be used");
		mockMvc.perform(post("/turns").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").value("Where would you like to stay?"))
				.andExpect(jsonPath("$.duplicate").value(true));
		org.junit.jupiter.api.Assertions.assertEquals(1, assistant.captured.size());
	}

	@Test
	void recommendIncludesStayOptions() throws Exception {
		assistant.next = BlAssistantResponse.recommend("Five places in Rome.", List.of(
				option("Alpha"), option("Beta"), option("Gamma"), option("Delta"), option("Epsilon")));
		mockMvc.perform(post("/turns")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"channel":"web","externalUserId":"bob","text":"Rome","eventId":"rome-1"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("recommend"))
				.andExpect(jsonPath("$.recommendations.length()").value(5))
				.andExpect(jsonPath("$.recommendations[0].name").value("Alpha"))
				.andExpect(jsonPath("$.recommendations[0].stayType").value("hotel"));
	}

	@Test
	void assistantFailureIs200FailNot500() throws Exception {
		assistant.failure = new BlAssistantException("LLM down");
		mockMvc.perform(post("/turns")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"channel":"web","externalUserId":"carol","text":"Rome","eventId":"fail-1"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("fail"))
				.andExpect(jsonPath("$.recommendations.length()").value(0));
	}

	@Test
	void historyIsEmptyForUnknownUserAndFilledAfterATurn() throws Exception {
		mockMvc.perform(get("/conversations/web/nobody"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages.length()").value(0));
		assistant.next = BlAssistantResponse.clarify("Where would you like to stay?");
		mockMvc.perform(post("/turns")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"channel":"web","externalUserId":"dave","text":"hello","eventId":"hist-1"}
						"""))
				.andExpect(status().isOk());
		mockMvc.perform(get("/conversations/web/dave"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages.length()").value(2))
				.andExpect(jsonPath("$.messages[0].role").value("user"))
				.andExpect(jsonPath("$.messages[0].text").value("hello"))
				.andExpect(jsonPath("$.messages[1].role").value("assistant"));
	}

	@Test
	void invalidHistoryChannelIs400() throws Exception {
		mockMvc.perform(get("/conversations/slack/alice"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}

	@TestConfiguration
	static class FakeAssistantConfig {

		@Bean
		@Primary
		ScriptedAssistantManager scriptedAssistantManager() {
			return new ScriptedAssistantManager();
		}
	}

	static class ScriptedAssistantManager implements IAssistantManager {

		BlAssistantResponse next = BlAssistantResponse.clarify("Where would you like to stay?");
		RuntimeException failure;
		final List<BlAssistantRequest> captured = new ArrayList<>();

		void reset() {
			next = BlAssistantResponse.clarify("Where would you like to stay?");
			failure = null;
			captured.clear();
		}

		@Override
		public BlAssistantResponse respond(BlAssistantRequest request) {
			captured.add(request);
			if (failure != null) {
				throw failure;
			}
			return next;
		}
	}
}
