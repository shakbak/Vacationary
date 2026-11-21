package com.vacationary.backend.domain.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.DbStayListing;

public class StayListingMapper {

	public BlStayOption toBl(DbStayListing listing, BlStaySearchQuery query, boolean fullMatch) {
		return new BlStayOption(
				listing.name(),
				listing.stayType(),
				listing.area() + ", " + listing.cityName(),
				StayDates.formatPrice(listing.pricePerNight(), query),
				formatRating(listing.rating()),
				buildWhy(listing, query, fullMatch),
				listing.url());
	}

	private static String formatRating(double rating) {
		return String.format(Locale.US, "%.1f", rating);
	}

	private static String buildWhy(DbStayListing listing, BlStaySearchQuery query, boolean fullMatch) {
		List<String> parts = new ArrayList<>();
		addIfPresent(parts, highlight(listing.amenities()));
		addIfPresent(parts, StayDates.staySummary(query));
		if (query.guestCount() != null) {
			parts.add("Sleeps " + listing.sleeps());
		}
		if (query.budgetMax() != null) {
			parts.add("$" + listing.pricePerNight() + " per night");
		}
		addIfPresent(parts, StayDates.tripTotal(query, listing.pricePerNight()));
		if (query.stayType() != null && listing.stayType() == query.stayType()) {
			parts.add("Requested stay type");
		}
		if (!fullMatch) {
			parts.add("Not a full match for this request");
		}
		if (parts.isEmpty()) {
			parts.add(listing.area() + " stay");
		}
		return String.join(". ", parts) + ".";
	}

	private static String highlight(String amenities) {
		if (amenities == null || amenities.isBlank()) {
			return null;
		}
		String trimmed = amenities.trim().replaceAll("\\s+", " ");
		return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
	}

	private static void addIfPresent(List<String> parts, String part) {
		if (part != null) {
			parts.add(part);
		}
	}
}
