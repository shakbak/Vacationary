package com.vacationary.backend.infra.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vacationary.backend.core.interfaces.recommendation.IStayProvider;
import com.vacationary.backend.domain.recommendation.StayDates;
import com.vacationary.backend.dtos.recommendation.BlStayOption;
import com.vacationary.backend.dtos.recommendation.BlStaySearchQuery;
import com.vacationary.backend.dtos.recommendation.StayType;

public class SampleStayProvider implements IStayProvider {

	private static final StayType[] TYPES = StayType.values();
	private static final String[] AREAS = { "Riverside", "Old Town", "Garden District", "Harbor", "Hillside" };
	private static final String[] NAME_PREFIXES = {
			"The", "Grand", "Harbor", "Cedar", "Maple"
	};
	private static final String[] NAME_SUFFIXES = {
			"Inn", "House", "Suites", "Lodge", "Residences"
	};

	@Override
	public List<BlStayOption> generate(BlStaySearchQuery query) {
		String city = displayCity(query.destination());
		int seed = Math.abs(city.toLowerCase(Locale.ROOT).hashCode());
		List<BlStayOption> options = new ArrayList<>(5);
		for (int i = 0; i < 5; i++) {
			StayType type = query.stayType() != null ? query.stayType() : TYPES[(seed + i) % TYPES.length];
			int price = priceFor(query, seed, i);
			int sleeps = query.guestCount() != null ? Math.max(query.guestCount(), 2) : 2 + ((seed + i) % 4);
			String area = AREAS[(seed + i) % AREAS.length];
			String name = NAME_PREFIXES[(seed + i) % NAME_PREFIXES.length] + " " + city + " "
					+ NAME_SUFFIXES[(seed + i * 3) % NAME_SUFFIXES.length];
			if (i > 0 && name.equals(options.get(0).name())) {
				name = name + " " + (i + 1);
			}
			String slug = slug(city) + "-" + (i + 1);
			options.add(new BlStayOption(
					name,
					type,
					area + ", " + city,
					StayDates.formatPrice(price, query),
					String.format(Locale.US, "%.1f", 4.0 + ((seed + i) % 9) / 10.0),
					why(type, area, city, query, sleeps, price),
					"https://www.airbnb.com/rooms/sample-" + slug));
		}
		ensureUniqueNames(options);
		return List.copyOf(options);
	}

	private static void ensureUniqueNames(List<BlStayOption> options) {
		for (int i = 0; i < options.size(); i++) {
			BlStayOption option = options.get(i);
			boolean duplicate = false;
			for (int j = 0; j < i; j++) {
				if (options.get(j).name().equals(option.name())) {
					duplicate = true;
					break;
				}
			}
			if (duplicate) {
				options.set(i, new BlStayOption(
						option.name() + " " + (i + 1),
						option.stayType(),
						option.location(),
						option.price(),
						option.rating(),
						option.why(),
						option.url()));
			}
		}
	}

	private static int priceFor(BlStaySearchQuery query, int seed, int index) {
		if (query.budgetMax() != null) {
			int step = Math.max(1, query.budgetMax() / 8);
			return Math.max(40, query.budgetMax() - index * step);
		}
		return 80 + ((seed + index * 17) % 180);
	}

	private static String why(
			StayType type,
			String area,
			String city,
			BlStaySearchQuery query,
			int sleeps,
			int price) {
		StringBuilder why = new StringBuilder(typeLabel(type) + " in " + area + ", " + city);
		appendIfPresent(why, StayDates.staySummary(query));
		if (query.guestCount() != null) {
			why.append(". Sleeps ").append(sleeps);
		}
		if (query.budgetMax() != null) {
			why.append(". $").append(price).append(" per night");
		}
		appendIfPresent(why, StayDates.tripTotal(query, price));
		why.append(".");
		return why.toString();
	}

	private static void appendIfPresent(StringBuilder why, String part) {
		if (part != null) {
			why.append(". ").append(part);
		}
	}

	private static String typeLabel(StayType type) {
		return switch (type) {
			case HOTEL -> "Hotel";
			case MOTEL -> "Motel";
			case VACATION_RENTAL -> "Vacation rental";
			case HOSTEL -> "Hostel";
			case BNB -> "B&B";
			case RESORT -> "Resort";
		};
	}

	private static String displayCity(String destination) {
		String trimmed = destination.trim().replaceAll("\\s+", " ");
		String[] parts = trimmed.split(" ");
		StringBuilder city = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}
			if (city.length() > 0) {
				city.append(' ');
			}
			city.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				city.append(part.substring(1).toLowerCase(Locale.ROOT));
			}
		}
		return city.toString();
	}

	private static String slug(String city) {
		return city.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
	}
}
