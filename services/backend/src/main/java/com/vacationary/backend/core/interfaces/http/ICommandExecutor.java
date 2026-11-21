package com.vacationary.backend.core.interfaces.http;

public interface ICommandExecutor<TBl, TResult> {

	TResult execute(TBl command);
}
