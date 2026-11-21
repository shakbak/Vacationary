package com.vacationary.backend.core.interfaces.http;

public interface IRequestMapper<TRequest, TBl> {

	TBl mapApiToBl(TRequest request);
}
