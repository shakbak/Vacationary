package com.vacationary.messaging.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.ObjectMapper;
import com.vacationary.messaging.core.interfaces.IMessageSender;
import com.vacationary.messaging.domain.inbound.InboundReplyManager;
import com.vacationary.messaging.domain.render.ReplyRenderer;
import com.vacationary.messaging.dtos.BlTurnResult;
import com.vacationary.messaging.infra.whatsapp.WhatsAppInboundParser;
import com.vacationary.messaging.infra.whatsapp.WhatsAppWebhookHandler;
import com.vacationary.messaging.infra.whatsapp.WhatsAppWebhookVerifier;

class WhatsAppWebhookControllerTest {

	private final List<String> sent = new ArrayList<>();
	private final IMessageSender sender = (to, body) -> sent.add(to + "|" + body);
	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(
			new WhatsAppWebhookHandler(
					new WhatsAppWebhookVerifier("secret"),
					new WhatsAppInboundParser(new ObjectMapper()),
					new InboundReplyManager(
							inbound -> new BlTurnResult("clarify", "Where would you like to stay?", List.of(), false),
							new ReplyRenderer()),
					sender))).build();

	@Test
	void verifyEchoesChallengeWhenTokenMatches() throws Exception {
		mvc.perform(get("/webhooks/whatsapp")
				.param("hub.mode", "subscribe")
				.param("hub.verify_token", "secret")
				.param("hub.challenge", "12345"))
				.andExpect(status().isOk())
				.andExpect(content().string("12345"));
	}

	@Test
	void verifyRejectsBadToken() throws Exception {
		mvc.perform(get("/webhooks/whatsapp")
				.param("hub.mode", "subscribe")
				.param("hub.verify_token", "nope")
				.param("hub.challenge", "12345"))
				.andExpect(status().isForbidden());
	}

	@Test
	void inboundTextReturnsOkAndSendsReply() throws Exception {
		String json = """
				{"entry":[{"changes":[{"value":{"messages":[{
				  "from":"15551234567","id":"wamid.1","type":"text","text":{"body":"a hotel"}}]}}]}]}
				""";
		mvc.perform(post("/webhooks/whatsapp").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());
		org.junit.jupiter.api.Assertions.assertEquals(
				List.of("15551234567|Where would you like to stay?"),
				sent);
	}
}
