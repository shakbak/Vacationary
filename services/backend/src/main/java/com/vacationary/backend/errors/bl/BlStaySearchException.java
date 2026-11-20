package com.vacationary.backend.errors.bl;

public class BlStaySearchException extends BlError {

	public BlStaySearchException(String message, Throwable cause) {
		super("STAY_SEARCH_FAILED", message);
		initCause(cause);
	}

	public BlStaySearchException(String message) {
		super("STAY_SEARCH_FAILED", message);
	}
}
