package com.vacationary.backend.domain.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.backend.core.interfaces.recommendation.IStayManager;
import com.vacationary.backend.domain.recommendation.StayListingMapper;
import com.vacationary.backend.domain.recommendation.StayManager;
import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlChatMessage;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.errors.bl.BlAssistantException;
import com.vacationary.backend.infra.llm.FakeLlmClient;
import com.vacationary.backend.infra.recommendation.SampleStayProvider;
import com.vacationary.backend.persistence.inmemory.recommendation.InMemoryStayRepository;

class AssistantManagerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final LlmPromptBuilder promptBuilder = new LlmPromptBuilder(objectMapper);
	private final AssistantResponseMapper responseMapper = new AssistantResponseMapper();
	private final List<BlStayOption> five = List.of(
			option("Alpha"), option("Bravo"), option("Charlie"), option("Delta"), option("Echo"));

	@Test
	void nullRequestThrows() {
		AssistantManager manager = manager(new FakeLlmClient(), query -> {
			throw new AssertionError("search must not run");
		});
		assertThrows(BlAssistantException.class, () -> manager.respond(null));
	}

	@Test
	void llmErrorIsFailWithNoRecommendations() {
		AssistantManager manager = manager(
				new FakeLlmClient(new BlLlmResponse.Error("timeout")),
				query -> {
					throw new AssertionError("search must not run");
				});
		BlAssistantResponse response = manager.respond(userTurn("Rome"));
		assertEquals(AssistantResponseKind.FAIL, response.kind());
		assertTrue(response.text().toLowerCase().contains("retry"));
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void clarifyTextHasNoRecommendations() {
		AssistantManager manager = manager(
				new FakeLlmClient(new BlLlmResponse.Text("CLARIFY: Where would you like to stay?")),
				query -> {
					throw new AssertionError("search must not run");
				});
		BlAssistantResponse response = manager.respond(userTurn("I need a place to stay"));
		assertEquals(AssistantResponseKind.CLARIFY, response.kind());
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void refuseScopeHasNoRecommendations() {
		AssistantManager manager = manager(
				new FakeLlmClient(new BlLlmResponse.Text("REFUSE_SCOPE: I only help with places to stay.")),
				query -> {
					throw new AssertionError("search must not run");
				});
		BlAssistantResponse response = manager.respond(userTurn("book me a flight"));
		assertEquals(AssistantResponseKind.REFUSE_SCOPE, response.kind());
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void toolCallWithoutDestinationClarifiesAndDoesNotSearch() {
		AtomicInteger searches = new AtomicInteger();
		AssistantManager manager = manager(
				new FakeLlmClient(new BlLlmResponse.ToolRequest("stay_search", "{}", "c1")),
				query -> {
					searches.incrementAndGet();
					return new StaySearchResult.Failure("should not run");
				});
		BlAssistantResponse response = manager.respond(userTurn("somewhere warm"));
		assertEquals(AssistantResponseKind.CLARIFY, response.kind());
		assertEquals(AssistantResponseMapper.DESTINATION_QUESTION, response.text());
		assertEquals(0, searches.get());
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void searchFailureIsFailNotRecommend() {
		AssistantManager manager = manager(
				new FakeLlmClient(new BlLlmResponse.ToolRequest("stay_search", "{\"destination\":\"Rome\"}", "c1")),
				query -> new StaySearchResult.Failure("catalog down"));
		BlAssistantResponse response = manager.respond(userTurn("Rome"));
		assertEquals(AssistantResponseKind.FAIL, response.kind());
		assertTrue(response.text().toLowerCase().contains("search"));
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void successfulSearchRecommendsToolOptionsNotModelInventedStays() {
		List<BlStaySearchQuery> queries = new ArrayList<>();
		FakeLlmClient llm = new FakeLlmClient(
				new BlLlmResponse.ToolRequest("stay_search", "{\"destination\":\"Rome\",\"stayType\":\"hotel\"}", "c1"),
				new BlLlmResponse.Text("Here are five hotels in Rome."));
		AssistantManager manager = manager(llm, query -> {
			queries.add(query);
			return new StaySearchResult.Success(five);
		});
		BlAssistantResponse response = manager.respond(userTurn("hotel in Rome"));
		assertEquals(AssistantResponseKind.RECOMMEND, response.kind());
		assertEquals(five.get(0).name(), response.recommendations().get(0).name());
		assertEquals(five.get(0).url(), response.recommendations().get(0).url());
		assertEquals(5, response.recommendations().size());
		assertEquals("Rome", queries.getFirst().destination());
		assertEquals(StayType.HOTEL, queries.getFirst().stayType());
		assertEquals(2, llm.capturedRequests().size());
		assertTrue(llm.capturedRequests().get(0).includeStaySearchTool());
		assertEquals(false, llm.capturedRequests().get(1).includeStaySearchTool());
	}

	@Test
	void secondLlmFailureStillRecommendsTheFive() {
		AssistantManager manager = manager(
				new FakeLlmClient(
						new BlLlmResponse.ToolRequest("stay_search", "{\"destination\":\"Rome\"}", "c1"),
						new BlLlmResponse.Error("write failed")),
				query -> new StaySearchResult.Success(five));
		BlAssistantResponse response = manager.respond(userTurn("Rome"));
		assertEquals(AssistantResponseKind.RECOMMEND, response.kind());
		assertEquals(AssistantResponseMapper.FALLBACK_RECOMMEND, response.text());
		assertEquals(5, response.recommendations().size());
	}

	@Test
	void unknownToolNameIsProcessFailure() {
		AssistantManager manager = manager(
				new FakeLlmClient(new BlLlmResponse.ToolRequest("weather", "{}", "c1")),
				query -> {
					throw new AssertionError("search must not run");
				});
		BlAssistantResponse response = manager.respond(userTurn("Rome"));
		assertEquals(AssistantResponseKind.FAIL, response.kind());
		assertTrue(response.recommendations().isEmpty());
	}

	@Test
	void realStayManagerReturnsFiveForRome() {
		StayManager stays = new StayManager(
				InMemoryStayRepository.withFixtures(),
				new SampleStayProvider(),
				new StayListingMapper());
		FakeLlmClient llm = new FakeLlmClient(
				new BlLlmResponse.ToolRequest("stay_search", "{\"destination\":\"Rome\"}", "c1"),
				new BlLlmResponse.Text("Five places in Rome."));
		AssistantManager manager = new AssistantManager(
				new LlmManager(promptBuilder, llm),
				new StaySearchParser(objectMapper),
				stays,
				responseMapper);
		BlAssistantResponse response = manager.respond(userTurn("Find me a hotel in Rome"));
		assertEquals(AssistantResponseKind.RECOMMEND, response.kind());
		assertEquals(5, response.recommendations().size());
		assertTrue(response.recommendations().stream().allMatch(option -> option.location().contains("Rome")));
	}

	private AssistantManager manager(FakeLlmClient llm, IStayManager stays) {
		return new AssistantManager(
				new LlmManager(promptBuilder, llm),
				new StaySearchParser(objectMapper),
				stays,
				responseMapper);
	}

	private static BlAssistantRequest userTurn(String text) {
		return new BlAssistantRequest(List.of(new BlChatMessage(ChatRole.USER, text)), text);
	}

	private static BlStayOption option(String name) {
		return new BlStayOption(name, StayType.HOTEL, "Rome", "$100 / night", "4.5", "why", "https://example.com/" + name);
	}
}
