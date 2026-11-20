package com.vacationary.backend.infra.llm;

import java.util.ArrayList;
import java.util.List;

import com.vacationary.backend.core.interfaces.assistant.ILlmClient;
import com.vacationary.backend.dtos.assistant.BlLlmRequest;
import com.vacationary.backend.dtos.assistant.BlLlmResponse;

public class FakeLlmClient implements ILlmClient {

	private final List<BlLlmResponse> script;
	private final List<BlLlmRequest> captured = new ArrayList<>();
	private int index;

	public FakeLlmClient(BlLlmResponse... script) {
		this.script = List.of(script);
	}

	@Override
	public BlLlmResponse complete(BlLlmRequest request) {
		captured.add(request);
		if (index >= script.size()) {
			return new BlLlmResponse.Error("FakeLlmClient has no more responses");
		}
		return script.get(index++);
	}

	public List<BlLlmRequest> capturedRequests() {
		return List.copyOf(captured);
	}
}
