package com.vacationary.backend.errors.bl;

public class BlError extends RuntimeException {

	private final String code;

	public BlError(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
