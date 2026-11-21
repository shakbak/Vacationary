package com.vacationary.backend.http.routes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vacationary.backend.core.interfaces.http.IRouteHandler;

@RestController
public class HttpRoutes {

	private final IRouteHandler turnsRouteHandler;
	private final IRouteHandler historyRouteHandler;

	public HttpRoutes(
			@Qualifier("turnsRouteHandler") IRouteHandler turnsRouteHandler,
			@Qualifier("historyRouteHandler") IRouteHandler historyRouteHandler) {
		this.turnsRouteHandler = turnsRouteHandler;
		this.historyRouteHandler = historyRouteHandler;
	}

	@PostMapping("/turns")
	public void postTurn(HttpServletRequest request, HttpServletResponse response) {
		turnsRouteHandler.handle(request, response);
	}

	@GetMapping("/conversations/{channel}/{externalUserId}")
	public void getConversation(HttpServletRequest request, HttpServletResponse response) {
		historyRouteHandler.handle(request, response);
	}
}
