package com.vacationary.backend.core.interfaces.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IRouteHandler {

	void handle(HttpServletRequest request, HttpServletResponse response);
}
