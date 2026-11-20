package com.vacationary.backend.core.interfaces.assistant;

import com.vacationary.backend.dtos.assistant.BlAssistantRequest;
import com.vacationary.backend.dtos.assistant.BlAssistantResponse;

public interface IAssistantManager {

	BlAssistantResponse respond(BlAssistantRequest request);
}
