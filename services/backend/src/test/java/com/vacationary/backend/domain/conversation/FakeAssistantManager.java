package com.vacationary.backend.domain.conversation;

import java.util.ArrayList;
import java.util.List;

import com.vacationary.backend.core.interfaces.assistant.IAssistantManager;
import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;

class FakeAssistantManager implements IAssistantManager {

	private final List<BlAssistantRequest> captured = new ArrayList<>();
	private BlAssistantResponse next = BlAssistantResponse.clarify("Where would you like to stay?");
	private RuntimeException failure;

	void replyWith(BlAssistantResponse response) {
		this.next = response;
		this.failure = null;
	}

	void failWith(RuntimeException error) {
		this.failure = error;
	}

	List<BlAssistantRequest> capturedRequests() {
		return List.copyOf(captured);
	}

	@Override
	public BlAssistantResponse respond(BlAssistantRequest request) {
		captured.add(request);
		if (failure != null) {
			throw failure;
		}
		return next;
	}
}
