package com.vacationary.backend.http.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.vacationary.backend.core.interfaces.http.ICommandExecutor;
import com.vacationary.backend.core.interfaces.http.IErrorHandler;
import com.vacationary.backend.core.interfaces.http.IHttpRequestReader;
import com.vacationary.backend.core.interfaces.http.IRequestMapper;
import com.vacationary.backend.core.interfaces.http.IResponseHandler;
import com.vacationary.backend.core.interfaces.http.IRouteHandler;
import com.vacationary.backend.core.interfaces.http.ISchemaValidator;

public class RouteHandler<TRequest, TBl, TResult> implements IRouteHandler {

	private final IHttpRequestReader<TRequest> requestReader;
	private final ISchemaValidator<TRequest> schemaValidator;
	private final IRequestMapper<TRequest, TBl> requestMapper;
	private final ICommandExecutor<TBl, TResult> commandExecutor;
	private final IResponseHandler<TResult> responseHandler;
	private final IErrorHandler errorHandler;

	public RouteHandler(
			IHttpRequestReader<TRequest> requestReader,
			ISchemaValidator<TRequest> schemaValidator,
			IRequestMapper<TRequest, TBl> requestMapper,
			ICommandExecutor<TBl, TResult> commandExecutor,
			IResponseHandler<TResult> responseHandler,
			IErrorHandler errorHandler) {
		this.requestReader = requestReader;
		this.schemaValidator = schemaValidator;
		this.requestMapper = requestMapper;
		this.commandExecutor = commandExecutor;
		this.responseHandler = responseHandler;
		this.errorHandler = errorHandler;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) {
		try {
			TRequest apiRequest = requestReader.read(request);
			schemaValidator.validate(apiRequest);
			TBl command = requestMapper.mapApiToBl(apiRequest);
			TResult result = commandExecutor.execute(command);
			responseHandler.handle(result, response);
		}
		catch (Exception error) {
			errorHandler.handle(error, response);
		}
	}
}
