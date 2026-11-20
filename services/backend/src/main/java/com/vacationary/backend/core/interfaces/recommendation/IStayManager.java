package com.vacationary.backend.core.interfaces.recommendation;

import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;

public interface IStayManager {

	StaySearchResult search(BlStaySearchQuery query);
}
