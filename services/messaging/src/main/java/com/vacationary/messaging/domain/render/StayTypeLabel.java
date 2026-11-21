package com.vacationary.messaging.domain.render;

import java.util.Locale;
import java.util.Map;

public final class StayTypeLabel {

	private static final Map<String, String> LABELS = Map.of(
			"hotel", "Hotel",
			"motel", "Motel",
			"vacation_rental", "Vacation rental",
			"hostel", "Hostel",
			"bnb", "B&B",
			"resort", "Resort");

	private StayTypeLabel() {
	}

	public static String of(String stayType) {
		if (stayType == null || stayType.isBlank()) {
			return "";
		}
		String key = stayType.trim().toLowerCase(Locale.ROOT);
		return LABELS.getOrDefault(key, stayType.trim());
	}
}
