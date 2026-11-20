package com.vacationary.backend.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vacationary.backend.core.interfaces.recommendation.IStayManager;
import com.vacationary.backend.core.interfaces.recommendation.IStayProvider;
import com.vacationary.backend.core.interfaces.recommendation.IStayRepository;
import com.vacationary.backend.domain.recommendation.StayListingMapper;
import com.vacationary.backend.domain.recommendation.StayManager;
import com.vacationary.backend.infra.recommendation.SampleStayProvider;
import com.vacationary.backend.persistence.inmemory.recommendation.InMemoryStayRepository;

@Configuration
public class RecommendationConfiguration {

	@Bean
	IStayRepository stayRepository() {
		return InMemoryStayRepository.withFixtures();
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
