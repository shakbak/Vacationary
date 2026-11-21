package com.vacationary.messaging.domain.inbound;

import com.vacationary.messaging.core.interfaces.IBackendClient;
import com.vacationary.messaging.core.interfaces.IMessageSender;
import com.vacationary.messaging.core.interfaces.IReplyRenderer;
import com.vacationary.messaging.core.interfaces.ITurnProcessor;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlTurnResult;

public class TurnProcessor implements ITurnProcessor {

	private final IBackendClient backendClient;
	private final IReplyRenderer replyRenderer;
	private final IMessageSender messageSender;

	public TurnProcessor(
			IBackendClient backendClient,
			IReplyRenderer replyRenderer,
			IMessageSender messageSender) {
		this.backendClient = backendClient;
		this.replyRenderer = replyRenderer;
		this.messageSender = messageSender;
	}

	@Override
	public void process(BlInboundMessage inbound) {
		if (inbound == null || inbound.externalUserId() == null || inbound.externalUserId().isBlank()) {
			return;
		}
		if (inbound.text() == null || inbound.text().isBlank()) {
			return;
		}
		BlTurnResult result = backendClient.postTurn(inbound);
		if (result != null && result.duplicate()) {
			return;
		}
		String body = replyRenderer.render(result);
		try {
			messageSender.send(inbound.externalUserId(), body);
		}
		catch (RuntimeException ignored) {
			// M-11: do not loop retries from here. Webhook still returns 200.
		}
	}
}
