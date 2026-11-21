package com.vacationary.backend.core.interfaces.http;

public interface ISchemaValidator<TRequest> {

	void validate(TRequest request);
}
