package com.vacationary.backend.errors.bl;

public class BlAssistantException extends BlError {

	public BlAssistantException(String message) {
		super("ASSISTANT_FAILED", message);
	}

	public BlAssistantException(String message, Throwable cause) {
		super("ASSISTANT_FAILED", message);
		initCause(cause);
	}
}
