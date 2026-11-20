package com.vacationary.backend.errors.bl;

public class BlValidationError extends BlError {

	public BlValidationError(String message) {
		super("VALIDATION_ERROR", message);
	}
}
