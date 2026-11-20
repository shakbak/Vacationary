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
				formatPrice(listing.pricePerNight()),
				formatRating(listing.rating()),
				buildWhy(listing, query, fullMatch),
				listing.url());
	}

	private static String formatPrice(int pricePerNight) {
		return "$" + pricePerNight + " / night";
	}

	private static String formatRating(double rating) {
		return String.format(Locale.US, "%.1f", rating);
	}

	private static String buildWhy(DbStayListing listing, BlStaySearchQuery query, boolean fullMatch) {
		List<String> parts = new ArrayList<>();
		parts.add(typeLabel(listing) + " in " + listing.area() + ", " + listing.cityName());
		if (query.guestCount() != null) {
			parts.add("Sleeps " + listing.sleeps());
		}
		if (query.budgetMax() != null) {
			parts.add("$" + listing.pricePerNight() + " per night");
		}
		if (query.stayType() != null && listing.stayType() == query.stayType()) {
			parts.add("Requested stay type");
		}
		if (!fullMatch) {
			parts.add("Not a full match for this request");
		}
		return String.join(". ", parts) + ".";
	}

	private static String typeLabel(DbStayListing listing) {
		return switch (listing.stayType()) {
			case HOTEL -> "Hotel";
			case MOTEL -> "Motel";
			case VACATION_RENTAL -> "Vacation rental";
			case HOSTEL -> "Hostel";
			case BNB -> "B&B";
			case RESORT -> "Resort";
		};
	}
}
