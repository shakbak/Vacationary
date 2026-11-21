package com.vacationary.messaging.infra.telegram;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TelegramBotClientTest {

	@Test
	void getUpdatesReadsResult() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://telegram");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(MockRestRequestMatchers.requestTo(containsString("/botsecret/getUpdates")))
				.andExpect(MockRestRequestMatchers.queryParam("timeout", "30"))
				.andExpect(MockRestRequestMatchers.queryParam("offset", "0"))
				.andRespond(MockRestResponseCreators.withSuccess(
						"""
								{"ok":true,"result":[{"update_id":1}]}
								""",
						MediaType.APPLICATION_JSON));
		TelegramBotClient client = new TelegramBotClient("http://telegram", "secret", new ObjectMapper(), builder.build());
		JsonNode root = client.getUpdates(0);
		assertTrue(root.path("ok").asBoolean());
		assertEquals(1, root.path("result").get(0).path("update_id").asInt());
	}

	@Test
	void sendPostsPlainText() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://telegram");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(MockRestRequestMatchers.requestTo("http://telegram/botsecret/sendMessage"))
				.andExpect(MockRestRequestMatchers.content().json("{\"chat_id\":\"4242\",\"text\":\"Where?\"}"))
				.andRespond(MockRestResponseCreators.withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
		TelegramBotClient client = new TelegramBotClient("http://telegram", "secret", new ObjectMapper(), builder.build());
		client.send("4242", "Where?");
		server.verify();
	}
}
