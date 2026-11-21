package com.vacationary.backend.core.interfaces.http;

import jakarta.servlet.http.HttpServletResponse;

public interface IErrorHandler {

	void handle(Exception error, HttpServletResponse response);
}
