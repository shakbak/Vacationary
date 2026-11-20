package com.vacationary.backend.persistence.inmemory.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vacationary.backend.core.interfaces.recommendation.IStayRepository;
import com.vacationary.backend.dtos.recommendation.DbStayListing;

public class InMemoryStayRepository implements IStayRepository {

	private final List<DbStayListing> listings;

	public InMemoryStayRepository(List<DbStayListing> listings) {
		this.listings = List.copyOf(listings);
	}

	public static InMemoryStayRepository withFixtures() {
		return new InMemoryStayRepository(CatalogFixtures.listings());
	}

	@Override
	public List<DbStayListing> findByDestination(String destination) {
		String key = normalize(destination);
		List<DbStayListing> found = new ArrayList<>();
		for (DbStayListing listing : listings) {
			if (listing.cityKey().equals(key)) {
				found.add(listing);
			}
		}
		return List.copyOf(found);
	}

	static String normalize(String destination) {
		return destination.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}
}
