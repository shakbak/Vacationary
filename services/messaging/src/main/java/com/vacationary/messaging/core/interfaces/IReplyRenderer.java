package com.vacationary.messaging.core.interfaces;

import com.vacationary.messaging.dtos.BlTurnResult;

public interface IReplyRenderer {

	String render(BlTurnResult result);
}
