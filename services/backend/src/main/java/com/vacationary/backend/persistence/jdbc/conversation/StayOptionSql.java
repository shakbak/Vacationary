package com.vacationary.backend.persistence.jdbc.conversation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.vacationary.backend.dtos.conversation.DbStayOption;
import com.vacationary.backend.dtos.recommendation.StayType;

final class StayOptionSql {

	static final String COLUMNS = "name, stay_type, location, price, rating, why, url";
	static final String QUALIFIED_COLUMNS =
			"stay_options.name, stay_options.stay_type, stay_options.location, stay_options.price, stay_options.rating, stay_options.why, stay_options.url";

	private StayOptionSql() {
	}

	static DbStayOption map(ResultSet rs) throws SQLException {
		return new DbStayOption(
				rs.getString("name"),
				StayType.valueOf(rs.getString("stay_type")),
				rs.getString("location"),
				rs.getString("price"),
				rs.getString("rating"),
				rs.getString("why"),
				rs.getString("url"));
	}

	static boolean hasOption(ResultSet rs) throws SQLException {
		return rs.getString("name") != null;
	}

	static void bind(PreparedStatement ps, int startIndex, DbStayOption option) throws SQLException {
		ps.setString(startIndex, option.name());
		ps.setString(startIndex + 1, option.stayType().name());
		ps.setString(startIndex + 2, option.location());
		ps.setString(startIndex + 3, option.price());
		ps.setString(startIndex + 4, option.rating());
		ps.setString(startIndex + 5, option.why());
		ps.setString(startIndex + 6, option.url());
	}

	static void deleteOwnedByConversation(JdbcTemplate jdbc, String channel, String externalUserId) {
		jdbc.update(
				"""
				DELETE FROM stay_options WHERE id IN (
					SELECT stay_option_id FROM (
						SELECT mso.stay_option_id
						FROM message_stay_options mso
						JOIN messages m ON m.id = mso.message_id
						WHERE m.channel = ? AND m.external_user_id = ?
					) owned
				)
				""",
				channel,
				externalUserId);
	}

	static void deleteOwnedByProcessedEvent(JdbcTemplate jdbc, String channel, String eventId) {
		jdbc.update(
				"""
				DELETE FROM stay_options WHERE id IN (
					SELECT stay_option_id FROM (
						SELECT stay_option_id
						FROM processed_event_stay_options
						WHERE channel = ? AND event_id = ?
					) owned
				)
				""",
				channel,
				eventId);
	}

	static List<DbStayOption> flatten(List<? extends List<DbStayOption>> groups) {
		return groups.stream().flatMap(List::stream).toList();
	}
}
