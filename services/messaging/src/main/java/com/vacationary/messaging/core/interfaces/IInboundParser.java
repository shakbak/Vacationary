package com.vacationary.messaging.core.interfaces;

import java.util.List;

import com.vacationary.messaging.dtos.BlInboundMessage;

public interface IInboundParser {

	List<BlInboundMessage> parse(String rawBody);
}
