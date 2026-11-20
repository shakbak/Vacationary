package com.vacationary.backend.bootstrap;

import javax.sql.DataSource;

import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vacationary.backend.core.interfaces.recommendation.IStayManager;
import com.vacationary.backend.core.interfaces.recommendation.IStayProvider;
import com.vacationary.backend.core.interfaces.recommendation.IStayRepository;
import com.vacationary.backend.domain.recommendation.StayListingMapper;
import com.vacationary.backend.domain.recommendation.StayManager;
import com.vacationary.backend.infra.recommendation.SampleStayProvider;
import com.vacationary.backend.persistence.jdbc.recommendation.JdbcStayRepository;

@Configuration
public class RecommendationConfiguration {

	@Bean
	@DependsOnDatabaseInitialization
	IStayRepository stayRepository(DataSource dataSource) {
		return new JdbcStayRepository(dataSource);
	}

	@Bean
	IStayProvider stayProvider() {
		return new SampleStayProvider();
	}

	@Bean
	StayListingMapper stayListingMapper() {
		return new StayListingMapper();
	}

	@Bean
	IStayManager stayManager(IStayRepository stayRepository, IStayProvider stayProvider, StayListingMapper stayListingMapper) {
		return new StayManager(stayRepository, stayProvider, stayListingMapper);
	}
}
