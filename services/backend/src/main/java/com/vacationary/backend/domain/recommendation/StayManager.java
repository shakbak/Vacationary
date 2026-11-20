package com.vacationary.backend.domain.recommendation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.vacationary.backend.core.interfaces.recommendation.IStayManager;
import com.vacationary.backend.core.interfaces.recommendation.IStayProvider;
import com.vacationary.backend.core.interfaces.recommendation.IStayRepository;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.DbStayListing;
import com.vacationary.backend.dtos.recommendation.StaySearchResult;
import com.vacationary.backend.errors.bl.BlStaySearchException;
import com.vacationary.backend.errors.bl.BlValidationError;

public class StayManager implements IStayManager {

	private static final int RESULT_COUNT = 5;

	private final IStayRepository stayRepository;
	private final IStayProvider stayProvider;
	private final StayListingMapper stayListingMapper;

	public StayManager(
			IStayRepository stayRepository,
			IStayProvider stayProvider,
			StayListingMapper stayListingMapper) {
		this.stayRepository = stayRepository;
		this.stayProvider = stayProvider;
		this.stayListingMapper = stayListingMapper;
	}

	@Override
	public StaySearchResult search(BlStaySearchQuery query) {
		if (query == null || query.destination() == null || query.destination().isBlank()) {
			throw new BlValidationError("destination is required");
		}
		try {
			List<DbStayListing> listings = stayRepository.findByDestination(query.destination());
			List<BlStayOption> options = listings.isEmpty()
					? new ArrayList<>(stayProvider.generate(query))
					: assembleFromRepository(listings, query);
			options = padToFive(options, query);
			if (options.size() < RESULT_COUNT) {
				return new StaySearchResult.Failure("Stay search failed");
			}
			return new StaySearchResult.Success(options.subList(0, RESULT_COUNT));
		}
		catch (BlValidationError error) {
			throw error;
		}
		catch (RuntimeException error) {
			return new StaySearchResult.Failure(new BlStaySearchException("Stay search failed", error).getMessage());
		}
	}

	private List<BlStayOption> assembleFromRepository(List<DbStayListing> listings, BlStaySearchQuery query) {
		List<BlStayOption> preferred = new ArrayList<>();
		List<BlStayOption> fallback = new ArrayList<>();
		for (DbStayListing listing : listings) {
			boolean match = matches(listing, query);
			BlStayOption option = stayListingMapper.toBl(listing, query, match);
			if (match) {
				preferred.add(option);
			}
			else {
				fallback.add(option);
			}
		}
		List<BlStayOption> ordered = new ArrayList<>(preferred);
		ordered.addAll(fallback);
		return ordered;
	}

	private List<BlStayOption> padToFive(List<BlStayOption> options, BlStaySearchQuery query) {
		if (options.size() >= RESULT_COUNT) {
			return options;
		}
		List<BlStayOption> padded = new ArrayList<>(options);
		Set<String> names = new LinkedHashSet<>();
		for (BlStayOption option : padded) {
			names.add(option.name().toLowerCase(Locale.ROOT));
		}
		for (BlStayOption generated : stayProvider.generate(query)) {
			if (padded.size() >= RESULT_COUNT) {
				break;
			}
			if (names.add(generated.name().toLowerCase(Locale.ROOT))) {
				padded.add(generated);
			}
		}
		return padded;
	}

	static boolean matches(DbStayListing listing, BlStaySearchQuery query) {
		if (query.stayType() != null && listing.stayType() != query.stayType()) {
			return false;
		}
		if (query.budgetMax() != null && listing.pricePerNight() > query.budgetMax()) {
			return false;
		}
		if (query.guestCount() != null && listing.sleeps() < query.guestCount()) {
			return false;
		}
		if (query.preferences() != null && !query.preferences().isBlank() && !preferencesMatch(listing, query.preferences())) {
			return false;
		}
		return true;
	}

	private static boolean preferencesMatch(DbStayListing listing, String preferences) {
		String haystack = (listing.amenities() + " " + listing.area() + " " + listing.name()).toLowerCase(Locale.ROOT);
		String[] tokens = preferences.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
		for (String token : tokens) {
			if (token.isBlank()) {
				continue;
			}
			if (haystack.contains(token)) {
				return true;
			}
		}
		return false;
	}
}
