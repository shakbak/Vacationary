package com.vacationary.backend.persistence.jdbc.recommendation;

import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;

import com.vacationary.backend.core.interfaces.recommendation.IStayRepository;
import com.vacationary.backend.dtos.recommendation.DbStayListing;
import com.vacationary.backend.dtos.recommendation.StayType;
import com.vacationary.backend.persistence.jdbc.BaseJdbcStore;

public class JdbcStayRepository extends BaseJdbcStore implements IStayRepository {

	private static final RowMapper<DbStayListing> LISTING_MAPPER = (rs, rowNum) -> new DbStayListing(
			rs.getString("city_key"),
			rs.getString("city_name"),
			rs.getString("name"),
			StayType.valueOf(rs.getString("stay_type")),
			rs.getString("area"),
			rs.getInt("price_per_night"),
			rs.getDouble("rating"),
			rs.getString("url"),
			rs.getInt("sleeps"),
			rs.getString("amenities"));

	public JdbcStayRepository(DataSource dataSource) {
		super(dataSource);
		seedIfEmpty(CatalogFixtures.listings());
	}

	@Override
	public List<DbStayListing> findByDestination(String destination) {
		if (destination == null || destination.isBlank()) {
			return List.of();
		}
		return jdbc.query(
				"""
				SELECT city_key, city_name, name, stay_type, area, price_per_night, rating, url, sleeps, amenities
				FROM stay_listings
				WHERE city_key = ?
				""",
				LISTING_MAPPER,
				normalize(destination));
	}

	static String normalize(String destination) {
		return destination.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

	private void seedIfEmpty(List<DbStayListing> listings) {
		if (listings.isEmpty()) {
			return;
		}
		jdbc.update(
				"""
				INSERT INTO stay_listings
					(city_key, city_name, name, stay_type, area, price_per_night, rating, url, sleeps, amenities)
				SELECT * FROM (VALUES 
				"""
						+ valueRows(listings.size(), 10)
						+ """
				) AS seed(city_key, city_name, name, stay_type, area, price_per_night, rating, url, sleeps, amenities)
				WHERE NOT EXISTS (SELECT 1 FROM stay_listings)
				""",
				ps -> {
					int index = 1;
					for (DbStayListing listing : listings) {
						ps.setString(index++, listing.cityKey());
						ps.setString(index++, listing.cityName());
						ps.setString(index++, listing.name());
						ps.setString(index++, listing.stayType().name());
						ps.setString(index++, listing.area());
						ps.setInt(index++, listing.pricePerNight());
						ps.setDouble(index++, listing.rating());
						ps.setString(index++, listing.url());
						ps.setInt(index++, listing.sleeps());
						ps.setString(index++, listing.amenities());
					}
				});
	}
}
