package com.vacationary.backend.persistence.jdbc.conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.vacationary.backend.core.interfaces.conversation.IProcessedEventRepository;
import com.vacationary.backend.dtos.assistant.AssistantResponseKind;
import com.vacationary.backend.dtos.conversation.BlEventKey;
import com.vacationary.backend.dtos.conversation.ConversationChannel;
import com.vacationary.backend.dtos.conversation.DbProcessedEvent;
import com.vacationary.backend.dtos.conversation.DbStayOption;
import com.vacationary.backend.persistence.jdbc.BaseJdbcStore;

public class JdbcProcessedEventStore extends BaseJdbcStore implements IProcessedEventRepository {

	public JdbcProcessedEventStore(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Optional<DbProcessedEvent> find(BlEventKey key) {
		if (key == null || key.channel() == null || key.eventId() == null) {
			return Optional.empty();
		}
		List<DbStayOption> options = new ArrayList<>();
		DbProcessedEvent[] found = { null };
		jdbc.query(
				"""
				SELECT pe.channel, pe.event_id, pe.kind, pe.text,
				"""
						+ StayOptionSql.QUALIFIED_COLUMNS
						+ """
				 FROM processed_events pe
				LEFT JOIN processed_event_stay_options peso
					ON peso.channel = pe.channel AND peso.event_id = pe.event_id
				LEFT JOIN stay_options ON stay_options.id = peso.stay_option_id
				WHERE pe.channel = ? AND pe.event_id = ?
				ORDER BY peso.seq
				""",
				ps -> {
					ps.setString(1, key.channel().name());
					ps.setString(2, key.eventId());
				},
				rs -> {
					if (found[0] == null) {
						found[0] = new DbProcessedEvent(
								ConversationChannel.valueOf(rs.getString("channel")),
								rs.getString("event_id"),
								AssistantResponseKind.valueOf(rs.getString("kind")),
								rs.getString("text"),
								List.of());
					}
					if (StayOptionSql.hasOption(rs)) {
						options.add(StayOptionSql.map(rs));
					}
				});
		if (found[0] == null) {
			return Optional.empty();
		}
		return Optional.of(new DbProcessedEvent(
				found[0].channel(),
				found[0].eventId(),
				found[0].kind(),
				found[0].text(),
				options));
	}

	@Override
	public void save(DbProcessedEvent event) {
		transactions.executeWithoutResult(status -> {
			String channel = event.channel().name();
			String eventId = event.eventId();
			StayOptionSql.deleteOwnedByProcessedEvent(jdbc, channel, eventId);
			jdbc.update("DELETE FROM processed_events WHERE channel = ? AND event_id = ?", channel, eventId);
			jdbc.update(
					"INSERT INTO processed_events (channel, event_id, kind, text) VALUES (?, ?, ?, ?)",
					channel,
					eventId,
					event.kind().name(),
					event.text());
			insertOptions(channel, eventId, event.recommendations());
		});
	}

	private void insertOptions(String channel, String eventId, List<DbStayOption> options) {
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
				"""
				INSERT INTO processed_event_stay_options
					(channel, event_id, seq, stay_option_id)
				VALUES 
				""" + valueRows(options.size(), 4),
				ps -> {
					int index = 1;
					for (int seq = 0; seq < options.size(); seq++) {
						ps.setString(index++, channel);
						ps.setString(index++, eventId);
						ps.setInt(index++, seq);
						ps.setLong(index++, optionIds.get(seq));
					}
				});
	}
}
