package com.vacationary.backend.persistence.jdbc;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class BaseJdbcStore {

	protected final JdbcTemplate jdbc;
	protected final TransactionTemplate transactions;

	protected BaseJdbcStore(DataSource dataSource) {
		this.jdbc = new JdbcTemplate(dataSource);
		this.transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
	}

	protected List<Long> insertReturningIds(int expectedRows, String sql, PreparedStatementSetter setter) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			setter.setValues(ps);
			return ps;
		}, keys);
		List<Long> ids = new ArrayList<>(keys.getKeyList().size());
		for (var row : keys.getKeyList()) {
			ids.add(((Number) row.values().iterator().next()).longValue());
		}
		if (ids.size() != expectedRows) {
			throw new IllegalStateException(
					"expected " + expectedRows + " generated ids, got " + ids.size());
		}
		return ids;
	}

	protected static String valueRows(int rows, int columns) {
		String row = "(" + String.join(", ", Collections.nCopies(columns, "?")) + ")";
		return String.join(", ", Collections.nCopies(rows, row));
	}
}
