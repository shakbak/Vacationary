package com.vacationary.messaging.dtos;

public record BlInboundMessage(String channel, String externalUserId, String text, String eventId) {
}
