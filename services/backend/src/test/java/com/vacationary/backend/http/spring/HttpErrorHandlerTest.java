package com.vacationary.backend.http.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.json.JsonMapper;

import com.vacationary.backend.constants.HttpStatuses;
import com.vacationary.backend.errors.bl.BlAssistantException;
import com.vacationary.backend.errors.bl.BlValidationError;

class HttpErrorHandlerTest {

	private final HttpResponseWriter writer = new HttpResponseWriter(JsonMapper.shared());
	private final HttpErrorHandler handler = new HttpErrorHandler(writer);

	@Test
	void validationBecomes400() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handle(new BlValidationError("text is required"), response);
		assertEquals(HttpStatuses.BAD_REQUEST, response.getStatus());
		assertTrue(response.getContentAsString().contains("VALIDATION_ERROR"));
		assertTrue(response.getContentAsString().contains("text is required"));
	}

	@Test
	void unexpectedBecomes500WithoutAHotelList() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.handle(new BlAssistantException("LLM down"), response);
		assertEquals(HttpStatuses.INTERNAL_SERVER_ERROR, response.getStatus());
		assertTrue(response.getContentAsString().contains("INTERNAL_ERROR"));
		assertTrue(response.getContentAsString().contains("An unexpected error occurred"));
		assertTrue(!response.getContentAsString().contains("hotel"));
	}
}
