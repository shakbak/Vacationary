package com.vacationary.backend.core.interfaces.recommendation;

import java.util.List;

import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;

public interface IStayProvider {

	List<BlStayOption> generate(BlStaySearchQuery query);
}
