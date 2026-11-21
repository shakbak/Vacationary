package com.vacationary.messaging.core.interfaces;

import java.util.Optional;

import com.vacationary.messaging.dtos.BlInboundMessage;
import com.vacationary.messaging.dtos.BlOutboundMessage;

public interface IInboundReplyManager {

	Optional<BlOutboundMessage> process(BlInboundMessage inbound);
}
