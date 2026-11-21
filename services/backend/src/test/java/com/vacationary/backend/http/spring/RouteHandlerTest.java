package com.vacationary.backend.http.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.json.JsonMapper;

import com.vacationary.backend.constants.HttpStatuses;
import com.vacationary.backend.core.interfaces.http.ICommandExecutor;
import com.vacationary.backend.dtos.http.ApiTurnRequest;
import com.vacationary.backend.http.features.turns.TurnsRequestMapper;
import com.vacationary.backend.http.features.turns.TurnsRequestReader;
import com.vacationary.backend.http.features.turns.TurnsSchemaValidator;

class RouteHandlerTest {

	@Test
	void invalidBodyDoesNotExecute() throws Exception {
		int[] executions = { 0 };
		ICommandExecutor<Object, Object> executor = command -> {
			executions[0]++;
			return command;
		};
		RouteHandler<ApiTurnRequest, Object, Object> handler = new RouteHandler<>(
				new TurnsRequestReader(JsonMapper.shared()),
				new TurnsSchemaValidator(),
				request -> {
					throw new IllegalStateException("mapper should not run");
				},
				executor,
				(result, response) -> {
					throw new IllegalStateException("response should not run");
				},
				new HttpErrorHandler(new HttpResponseWriter(JsonMapper.shared())));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContent("{}".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handle(request, response);
		assertEquals(0, executions[0]);
		assertEquals(HttpStatuses.BAD_REQUEST, response.getStatus());
		assertTrue(response.getContentAsString().contains("VALIDATION_ERROR"));
	}

	@Test
	void mapperIsUsedAfterValidation() {
		TurnsRequestMapper mapper = new TurnsRequestMapper();
		assertEquals(
				"alice",
				mapper.mapApiToBl(new ApiTurnRequest("web", "alice", "Rome", "evt-1")).conversation().externalUserId());
	}
}
