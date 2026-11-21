package com.vacationary.backend.dtos.http;

public record ApiTurnRequest(String channel, String externalUserId, String text, String eventId) {
}
