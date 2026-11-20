package com.vacationary.backend.persistence.jdbc;

import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public final class H2TestDataSource {

	private H2TestDataSource() {
	}

	public static DataSource create() {
		String name = "vacationary_" + UUID.randomUUID().toString().replace("-", "");
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl(
				"jdbc:h2:mem:" + name
						+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
		populator.execute(dataSource);
		return dataSource;
	}
}
