package com.vacationary.backend.persistence.jdbc.conversation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import com.vacationary.backend.core.interfaces.conversation.IConversationRepository;
import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.assistant.ChatRole;
import com.vacationary.backend.dtos.conversation.BlConversationKey;
import com.vacationary.backend.dtos.conversation.DbConversation;
import com.vacationary.backend.dtos.conversation.DbMessage;
import com.vacationary.backend.dtos.conversation.DbStayOption;
import com.vacationary.backend.persistence.jdbc.BaseJdbcStore;

public class JdbcConversationStore extends BaseJdbcStore implements IConversationRepository {

	public JdbcConversationStore(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Optional<DbConversation> find(BlConversationKey key) {
		if (key == null || key.channel() == null || key.externalUserId() == null) {
			return Optional.empty();
		}
		Map<Long, MessageDraft> messages = new LinkedHashMap<>();
		boolean[] found = { false };
		jdbc.query(
				"""
				SELECT m.id AS message_id, m.seq AS message_seq, m.role, m.text, m.time, m.outcome_kind,
				"""
						+ StayOptionSql.QUALIFIED_COLUMNS
						+ """
				 FROM conversations c
				LEFT JOIN messages m
					ON m.channel = c.channel AND m.external_user_id = c.external_user_id
				LEFT JOIN message_stay_options mso ON mso.message_id = m.id
				LEFT JOIN stay_options ON stay_options.id = mso.stay_option_id
				WHERE c.channel = ? AND c.external_user_id = ?
				ORDER BY m.seq, mso.seq
				""",
				ps -> {
					ps.setString(1, key.channel().name());
					ps.setString(2, key.externalUserId());
				},
				rs -> {
					found[0] = true;
					Long messageId = rs.getObject("message_id", Long.class);
					if (messageId == null) {
						return;
					}
					MessageDraft draft = messages.get(messageId);
					if (draft == null) {
						draft = new MessageDraft(
								ChatRole.valueOf(rs.getString("role")),
								rs.getString("text"),
								rs.getTimestamp("time").toInstant(),
								kind(rs.getString("outcome_kind")),
								new ArrayList<>());
						messages.put(messageId, draft);
					}
					if (StayOptionSql.hasOption(rs)) {
						draft.options().add(StayOptionSql.map(rs));
					}
				});
		if (!found[0]) {
			return Optional.empty();
		}
		List<DbMessage> copied = new ArrayList<>();
		for (MessageDraft draft : messages.values()) {
			copied.add(new DbMessage(
					draft.role(),
					draft.text(),
					draft.time(),
					draft.outcomeKind(),
					draft.options()));
		}
		return Optional.of(new DbConversation(key.channel(), key.externalUserId(), copied));
	}

	@Override
	public void save(DbConversation conversation) {
		transactions.executeWithoutResult(status -> {
			String channel = conversation.channel().name();
			String externalUserId = conversation.externalUserId();
			jdbc.update(
					"""
					INSERT INTO conversations (channel, external_user_id)
					SELECT ?, ?
					WHERE NOT EXISTS (
						SELECT 1 FROM conversations WHERE channel = ? AND external_user_id = ?
					)
					""",
					channel,
					externalUserId,
					channel,
					externalUserId);
			StayOptionSql.deleteOwnedByConversation(jdbc, channel, externalUserId);
			jdbc.update(
					"DELETE FROM messages WHERE channel = ? AND external_user_id = ?",
					channel,
					externalUserId);
			List<DbMessage> messages = conversation.messages();
			if (messages.isEmpty()) {
				return;
			}
			List<Long> messageIds = insertMessages(channel, externalUserId, messages);
			if (messageIds.size() != messages.size()) {
				throw new IllegalStateException(
						"expected " + messages.size() + " message ids, got " + messageIds.size());
			}
			insertMessageOptions(messageIds, messages);
		});
	}

	private List<Long> insertMessages(String channel, String externalUserId, List<DbMessage> messages) {
		jdbc.update(
				"""
				INSERT INTO messages
					(channel, external_user_id, seq, role, text, time, outcome_kind)
				VALUES 
				""" + valueRows(messages.size(), 7),
				ps -> {
					int index = 1;
					for (int seq = 0; seq < messages.size(); seq++) {
						DbMessage message = messages.get(seq);
						ps.setString(index++, channel);
						ps.setString(index++, externalUserId);
						ps.setInt(index++, seq);
						ps.setString(index++, message.role().name());
						ps.setString(index++, message.text());
						ps.setTimestamp(index++, Timestamp.from(message.time()));
						ps.setString(index++, message.outcomeKind() == null ? null : message.outcomeKind().name());
					}
				});
		return jdbc.query(
				"""
				SELECT id FROM messages
				WHERE channel = ? AND external_user_id = ?
				ORDER BY seq
				""",
				(rs, rowNum) -> rs.getLong("id"),
				channel,
				externalUserId);
	}

	private void insertMessageOptions(List<Long> messageIds, List<DbMessage> messages) {
		List<DbStayOption> options = StayOptionSql.flatten(
				messages.stream().map(DbMessage::recommendations).toList());
		if (options.isEmpty()) {
			return;
		}
		List<Long> optionIds = insertReturningIds(
				options.size(),
				"INSERT INTO stay_options (" + StayOptionSql.COLUMNS + ") VALUES " + valueRows(options.size(), 7),
				ps -> {
					int index = 1;
					for (DbStayOption option : options) {
						StayOptionSql.bind(ps, index, option);
						index += 7;
					}
				});
		jdbc.update(
				"INSERT INTO message_stay_options (message_id, seq, stay_option_id) VALUES "
						+ valueRows(options.size(), 3),
				ps -> {
					int index = 1;
					int optionIndex = 0;
					for (int messageIndex = 0; messageIndex < messages.size(); messageIndex++) {
						List<DbStayOption> messageOptions = messages.get(messageIndex).recommendations();
						for (int seq = 0; seq < messageOptions.size(); seq++) {
							ps.setLong(index++, messageIds.get(messageIndex));
							ps.setInt(index++, seq);
							ps.setLong(index++, optionIds.get(optionIndex++));
						}
					}
				});
	}

	private static AssistantResponseKind kind(String value) {
		return value == null ? null : AssistantResponseKind.valueOf(value);
	}

	private record MessageDraft(
			ChatRole role,
			String text,
			Instant time,
			AssistantResponseKind outcomeKind,
			List<DbStayOption> options) {
	}
}
