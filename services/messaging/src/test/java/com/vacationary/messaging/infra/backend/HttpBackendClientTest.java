package com.vacationary.messaging.infra.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.constants.Channels;
import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlTurnResult;

class HttpBackendClientTest {

	@Test
	void mapsRecommendJson() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://backend");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(MockRestRequestMatchers.requestTo("http://backend/turns"))
				.andRespond(MockRestResponseCreators.withSuccess(
						"""
								{"outcome":"recommend","text":"Here you go.","recommendations":[{
								  "name":"Alpha Inn","stayType":"hotel","location":"Rome",
								  "price":"$100 / night","rating":"4.5","why":"Quiet","url":"https://example.com/a"
								}],"duplicate":false}
								""",
						MediaType.APPLICATION_JSON));
		HttpBackendClient client = new HttpBackendClient("http://backend", new ObjectMapper(), builder.build());
		BlTurnResult result = client.postTurn(
				new BlInboundMessage(Channels.TELEGRAM, "4242", "Rome", "9001"));
		assertEquals("recommend", result.outcome());
		assertEquals("Here you go.", result.text());
		assertFalse(result.duplicate());
		assertEquals("Alpha Inn", result.recommendations().get(0).name());
		assertEquals("Quiet", result.recommendations().get(0).why());
	}

	@Test
	void transportFailureIsFailOutcome() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://backend");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(MockRestRequestMatchers.requestTo("http://backend/turns"))
				.andRespond(MockRestResponseCreators.withServerError());
		HttpBackendClient client = new HttpBackendClient("http://backend", new ObjectMapper(), builder.build());
		BlTurnResult result = client.postTurn(
				new BlInboundMessage(Channels.TELEGRAM, "4242", "Rome", "9001"));
		assertEquals("fail", result.outcome());
		assertEquals(ReplyCopy.BACKEND_UNREACHABLE, result.text());
		assertTrue(result.recommendations().isEmpty());
	}
}
