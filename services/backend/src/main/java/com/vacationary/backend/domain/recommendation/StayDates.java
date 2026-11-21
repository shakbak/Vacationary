package com.vacationary.backend.domain.recommendation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;

public final class StayDates {

	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	private StayDates() {
	}

	public static int nights(BlStaySearchQuery query) {
		if (query == null || query.checkIn() == null || query.checkOut() == null) {
			return 0;
		}
		long nights = ChronoUnit.DAYS.between(query.checkIn(), query.checkOut());
		if (nights <= 0 || nights > Integer.MAX_VALUE) {
			return 0;
		}
		return (int) nights;
	}

	public static String formatPrice(int pricePerNight, BlStaySearchQuery query) {
		String nightly = "$" + pricePerNight + " / night";
		int nights = nights(query);
		if (nights == 0) {
			return nightly;
		}
		return nightly + " · " + nights + (nights == 1 ? " night" : " nights");
	}

	public static String staySummary(BlStaySearchQuery query) {
		int nights = nights(query);
		if (nights == 0) {
			return null;
		}
		return nightsLabel(nights) + ", " + formatRange(query.checkIn(), query.checkOut());
	}

	public static String tripTotal(BlStaySearchQuery query, int pricePerNight) {
		int nights = nights(query);
		if (nights == 0 || query.budgetMax() == null) {
			return null;
		}
		return "About $" + (pricePerNight * (long) nights) + " for the stay";
	}

	static String formatRange(LocalDate checkIn, LocalDate checkOut) {
		if (checkIn.getYear() != checkOut.getYear()) {
			return checkIn.format(DAY_MONTH_YEAR) + "–" + checkOut.format(DAY_MONTH_YEAR);
		}
		if (checkIn.getMonth() == checkOut.getMonth()) {
			return checkIn.getDayOfMonth() + "–" + checkOut.format(DAY_MONTH);
		}
		return checkIn.format(DAY_MONTH) + "–" + checkOut.format(DAY_MONTH);
	}

	private static String nightsLabel(int nights) {
		return nights == 1 ? "1 night" : nights + " nights";
	}
}
