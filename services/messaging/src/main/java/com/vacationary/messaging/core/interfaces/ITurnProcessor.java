package com.vacationary.messaging.core.interfaces;

import com.vacationary.messaging.dtos.BlInboundMessage;

public interface ITurnProcessor {

	void process(BlInboundMessage inbound);
}
