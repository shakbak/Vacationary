package com.vacationary.backend.domain.conversation;

import java.util.ArrayList;
import java.util.List;

import com.vacationary.backend.dtos.conversation.BlConversation;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.BlMessage;
import com.vacationary.backend.dtos.conversation.DbConversation;
import com.vacationary.backend.dtos.conversation.DbMessage;
import com.vacationary.backend.dtos.conversation.DbStayOption;
import com.vacationary.backend.dtos.recommendation.BlStayOption;

public class ConversationMapper {

	public BlConversation toBl(DbConversation row) {
		if (row == null) {
			return null;
		}
		List<BlMessage> messages = new ArrayList<>();
		for (DbMessage message : row.messages()) {
			messages.add(new BlMessage(
					message.role(),
					message.text(),
					message.time(),
					message.outcomeKind(),
					toBlOptions(message.recommendations())));
		}
		return new BlConversation(
				new BlConversationKey(row.channel(), row.externalUserId()),
				messages);
	}

	public DbConversation toDb(BlConversation conversation) {
		List<DbMessage> messages = new ArrayList<>();
		for (BlMessage message : conversation.messages()) {
			messages.add(new DbMessage(
					message.role(),
					message.text(),
					message.time(),
					message.outcomeKind(),
					toDbOptions(message.recommendations())));
		}
		return new DbConversation(
				conversation.key().channel(),
				conversation.key().externalUserId(),
				messages);
	}

	static List<DbStayOption> toDbOptions(List<BlStayOption> options) {
		if (options == null || options.isEmpty()) {
			return List.of();
		}
		List<DbStayOption> copied = new ArrayList<>();
		for (BlStayOption option : options) {
			copied.add(new DbStayOption(
					option.name(),
					option.stayType(),
					option.location(),
					option.price(),
					option.rating(),
					option.why(),
					option.url()));
		}
		return copied;
	}

	static List<BlStayOption> toBlOptions(List<DbStayOption> options) {
		if (options == null || options.isEmpty()) {
			return List.of();
		}
		List<BlStayOption> copied = new ArrayList<>();
		for (DbStayOption option : options) {
			copied.add(new BlStayOption(
					option.name(),
					option.stayType(),
					option.location(),
					option.price(),
					option.rating(),
					option.why(),
					option.url()));
		}
		return copied;
	}
}
