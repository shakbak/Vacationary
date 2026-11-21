package com.vacationary.messaging.core.interfaces;

import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlTurnResult;

public interface IBackendClient {

	BlTurnResult postTurn(BlInboundMessage inbound);
}
