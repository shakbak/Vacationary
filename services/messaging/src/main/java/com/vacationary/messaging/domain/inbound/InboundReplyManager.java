package com.vacationary.messaging.domain.inbound;

import java.util.Optional;

import com.vacationary.messaging.core.interfaces.IBackendClient;
import com.vacationary.messaging.core.interfaces.IInboundReplyManager;
import com.vacationary.messaging.core.interfaces.IReplyRenderer;
import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlOutboundMessage;
import com.vacationary.messaging.dtos.BlTurnResult;

public class InboundReplyManager implements IInboundReplyManager {

	private final IBackendClient backendClient;
	private final IReplyRenderer replyRenderer;

	public InboundReplyManager(IBackendClient backendClient, IReplyRenderer replyRenderer) {
		this.backendClient = backendClient;
		this.replyRenderer = replyRenderer;
	}

	@Override
	public Optional<BlOutboundMessage> process(BlInboundMessage inbound) {
		if (inbound == null || isBlank(inbound.externalUserId()) || isBlank(inbound.text())) {
			return Optional.empty();
		}
		BlTurnResult result = backendClient.postTurn(inbound);
		if (result != null && result.duplicate()) {
			return Optional.empty();
		}
		String body = replyRenderer.render(result);
		return Optional.of(new BlOutboundMessage(inbound.externalUserId(), body));
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
