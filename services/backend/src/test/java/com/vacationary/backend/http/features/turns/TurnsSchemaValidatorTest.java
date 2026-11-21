package com.vacationary.backend.http.features.turns;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.vacationary.backend.dtos.http.ApiTurnRequest;
import com.vacationary.backend.errors.bl.BlValidationError;

class TurnsSchemaValidatorTest {

	private final TurnsSchemaValidator validator = new TurnsSchemaValidator();

	@Test
	void validRequestPasses() {
		assertDoesNotThrow(() -> validator.validate(new ApiTurnRequest("web", "alice", "Rome", "evt-1")));
		assertDoesNotThrow(() -> validator.validate(new ApiTurnRequest("TELEGRAM", "alice", "Rome", "evt-1")));
	}

	@Test
	void missingOrBlankFieldsFail() {
		assertThrows(BlValidationError.class, () -> validator.validate(null));
		assertThrows(BlValidationError.class, () -> validator.validate(new ApiTurnRequest("  ", "alice", "Rome", "evt-1")));
		assertThrows(BlValidationError.class, () -> validator.validate(new ApiTurnRequest("web", "  ", "Rome", "evt-1")));
		assertThrows(BlValidationError.class, () -> validator.validate(new ApiTurnRequest("web", "alice", "  ", "evt-1")));
		assertThrows(BlValidationError.class, () -> validator.validate(new ApiTurnRequest("web", "alice", "Rome", "  ")));
	}

	@Test
	void unknownChannelFails() {
		assertThrows(BlValidationError.class, () -> validator.validate(
				new ApiTurnRequest("slack", "alice", "Rome", "evt-1")));
	}
}
