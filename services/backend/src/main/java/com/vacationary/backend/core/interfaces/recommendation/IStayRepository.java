package com.vacationary.backend.core.interfaces.recommendation;

import java.util.List;

import com.vacationary.backend.dtos.recommendation.DbStayListing;

public interface IStayRepository {

	List<DbStayListing> findByDestination(String destination);
}
