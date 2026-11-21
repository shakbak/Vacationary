package com.vacationary.backend.domain.conversation;

import java.util.Optional;

import com.vacationary.backend.core.interfaces.conversation.IProcessedEventManager;
import com.vacationary.backend.core.interfaces.conversation.IProcessedEventRepository;
import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.BlProcessedEvent;
import com.vacationary.backend.errors.bl.BlValidationError;

public class ProcessedEventManager implements IProcessedEventManager {

	private final IProcessedEventRepository processedEventRepository;
	private final ProcessedEventMapper processedEventMapper;

	public ProcessedEventManager(
			IProcessedEventRepository processedEventRepository,
			ProcessedEventMapper processedEventMapper) {
		this.processedEventRepository = processedEventRepository;
		this.processedEventMapper = processedEventMapper;
	}

	@Override
	public Optional<BlProcessedEvent> getEvent(BlEventKey key) {
		BlEventKey normalized = EventValidator.validate(key);
		return processedEventRepository.find(normalized).map(processedEventMapper::toBl);
	}

	@Override
	public void save(BlProcessedEvent event) {
		if (event == null || event.outcome() == null) {
			throw new BlValidationError("processed event is required");
		}
		EventValidator.validateOutcome(event.outcome().kind(), event.outcome().text());
		BlEventKey key = EventValidator.validate(event.key());
		processedEventRepository.save(processedEventMapper.toDb(new BlProcessedEvent(key, event.outcome())));
	}

	private static final class EventValidator {

		private EventValidator() {
		}

		static BlEventKey validate(BlEventKey key) {
			if (key == null) {
				throw new BlValidationError("key is required");
			}
			if (key.channel() == null) {
				throw new BlValidationError("channel is required");
			}
			String eventId = trimToNull(key.eventId());
			if (eventId == null) {
				throw new BlValidationError("eventId is required");
			}
			return new BlEventKey(key.channel(), eventId);
		}

		static void validateOutcome(AssistantResponseKind kind, String text) {
			if (kind == null) {
				throw new BlValidationError("outcome kind is required");
			}
			if (trimToNull(text) == null) {
				throw new BlValidationError("outcome text is required");
			}
		}

		private static String trimToNull(String value) {
			if (value == null) {
				return null;
			}
			String trimmed = value.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
	}
}
