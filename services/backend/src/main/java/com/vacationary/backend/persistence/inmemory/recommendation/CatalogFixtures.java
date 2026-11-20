package com.vacationary.backend.persistence.inmemory.recommendation;

import java.util.List;
import java.util.Locale;

import com.vacationary.backend.dtos.recommendation.DbStayListing;
import com.vacationary.backend.dtos.recommendation.StayType;

final class CatalogFixtures {

	private CatalogFixtures() {
	}

	static List<DbStayListing> listings() {
		return List.of(
				rome("Hotel Campo de' Fiori", StayType.HOTEL, "Centro Storico", 210, 4.6, "campo-fiori", 3, "quiet family"),
				rome("Trastevere Riverside Hostel", StayType.HOSTEL, "Trastevere", 45, 4.2, "trastevere-hostel", 8, "social family"),
				rome("Aventine Garden B&B", StayType.BNB, "Aventine", 130, 4.7, "aventine-bnb", 2, "quiet garden"),
				rome("Ostia Beach Motel", StayType.MOTEL, "Ostia", 85, 3.9, "ostia-motel", 4, "beach parking"),
				rome("Pantheon Loft Rental", StayType.VACATION_RENTAL, "Pantheon", 175, 4.5, "pantheon-loft", 5, "family kitchen"),
				rome("Villa Borghese Resort", StayType.RESORT, "Parioli", 320, 4.8, "borghese-resort", 4, "pool quiet"),
				rome("Testaccio Budget Hotel", StayType.HOTEL, "Testaccio", 95, 4.0, "testaccio-hotel", 2, "local"),
				rome("Gianicolo Family Hostel", StayType.HOSTEL, "Gianicolo", 38, 4.1, "gianicolo-hostel", 6, "family view"),
				paris("Hotel Le Marais Noble", StayType.HOTEL, "Le Marais", 240, 4.7, "marais-noble", 2, "quiet design"),
				paris("Canal Saint-Martin Hostel", StayType.HOSTEL, "Canal Saint-Martin", 52, 4.3, "canal-hostel", 10, "social"),
				paris("Montmartre Hill B&B", StayType.BNB, "Montmartre", 145, 4.6, "montmartre-bnb", 2, "view quiet"),
				paris("Porte de Versailles Motel", StayType.MOTEL, "15th arrondissement", 78, 3.8, "versailles-motel", 3, "parking"),
				paris("Seine Apartment Rental", StayType.VACATION_RENTAL, "Saint-Germain", 195, 4.4, "seine-apt", 4, "family river"),
				paris("Bois de Boulogne Resort", StayType.RESORT, "16th arrondissement", 340, 4.8, "boulogne-resort", 4, "park pool"),
				lisbon("Alfama Tile Hotel", StayType.HOTEL, "Alfama", 160, 4.5, "alfama-tile", 3, "view quiet"),
				lisbon("Bairro Alto Hostel", StayType.HOSTEL, "Bairro Alto", 35, 4.2, "bairro-hostel", 12, "nightlife social"),
				lisbon("Belem River B&B", StayType.BNB, "Belém", 110, 4.6, "belem-bnb", 2, "quiet river"),
				lisbon("Parque das Nações Motel", StayType.MOTEL, "Parque das Nações", 70, 3.9, "nacoes-motel", 4, "modern parking"),
				lisbon("Chiado Flat Rental", StayType.VACATION_RENTAL, "Chiado", 150, 4.4, "chiado-flat", 5, "family kitchen"),
				lisbon("Cascais Coast Resort", StayType.RESORT, "Cascais", 280, 4.7, "cascais-resort", 4, "beach pool"),
				barcelona("Eixample Gallery Hotel", StayType.HOTEL, "Eixample", 190, 4.5, "eixample-gallery", 3, "design quiet"),
				barcelona("El Raval Hostel", StayType.HOSTEL, "El Raval", 40, 4.1, "raval-hostel", 10, "social"),
				barcelona("Gracia Terrace B&B", StayType.BNB, "Gràcia", 125, 4.6, "gracia-bnb", 2, "quiet terrace"),
				barcelona("Airport Access Motel", StayType.MOTEL, "El Prat", 65, 3.7, "prat-motel", 3, "parking"),
				barcelona("Barceloneta Beach Rental", StayType.VACATION_RENTAL, "Barceloneta", 170, 4.3, "barceloneta-rental", 6, "beach family"),
				barcelona("Sitges Bay Resort", StayType.RESORT, "Sitges", 300, 4.7, "sitges-resort", 4, "beach pool"),
				tokyo("Yanaka Wooden Hotel", StayType.HOTEL, "Yanaka", 180, 4.6, "yanaka-hotel", 2, "quiet traditional"),
				tokyo("Asakusa Capsule Hostel", StayType.HOSTEL, "Asakusa", 42, 4.2, "asakusa-hostel", 1, "central"),
				tokyo("Shimokitazawa B&B", StayType.BNB, "Shimokitazawa", 135, 4.5, "shimo-bnb", 2, "local quiet"),
				tokyo("Haneda Airport Motel", StayType.MOTEL, "Ota", 88, 3.8, "haneda-motel", 3, "parking airport"),
				tokyo("Koto Family Rental", StayType.VACATION_RENTAL, "Koto", 165, 4.4, "koto-rental", 6, "family kitchen"),
				tokyo("Odaiba Bay Resort", StayType.RESORT, "Odaiba", 310, 4.7, "odaiba-resort", 4, "view pool"),
				newYork("Chelsea Loft Hotel", StayType.HOTEL, "Chelsea", 260, 4.5, "chelsea-loft", 2, "design"),
				newYork("Harlem Hostel House", StayType.HOSTEL, "Harlem", 55, 4.1, "harlem-hostel", 8, "social family"),
				newYork("Park Slope B&B", StayType.BNB, "Park Slope", 155, 4.6, "park-slope-bnb", 2, "quiet brownstone"),
				newYork("JFK Airport Motel", StayType.MOTEL, "Queens", 92, 3.7, "jfk-motel", 4, "parking airport"),
				newYork("Williamsburg Rental", StayType.VACATION_RENTAL, "Williamsburg", 210, 4.4, "williamsburg-rental", 5, "family loft"),
				newYork("Rockaway Beach Resort", StayType.RESORT, "Rockaway", 275, 4.3, "rockaway-resort", 4, "beach"));
	}

	private static DbStayListing rome(String name, StayType type, String area, int price, double rating, String slug, int sleeps, String amenities) {
		return listing("Rome", name, type, area, price, rating, slug, sleeps, amenities);
	}

	private static DbStayListing paris(String name, StayType type, String area, int price, double rating, String slug, int sleeps, String amenities) {
		return listing("Paris", name, type, area, price, rating, slug, sleeps, amenities);
	}

	private static DbStayListing lisbon(String name, StayType type, String area, int price, double rating, String slug, int sleeps, String amenities) {
		return listing("Lisbon", name, type, area, price, rating, slug, sleeps, amenities);
	}

	private static DbStayListing barcelona(String name, StayType type, String area, int price, double rating, String slug, int sleeps, String amenities) {
		return listing("Barcelona", name, type, area, price, rating, slug, sleeps, amenities);
	}

	private static DbStayListing tokyo(String name, StayType type, String area, int price, double rating, String slug, int sleeps, String amenities) {
		return listing("Tokyo", name, type, area, price, rating, slug, sleeps, amenities);
	}

	private static DbStayListing newYork(String name, StayType type, String area, int price, double rating, String slug, int sleeps, String amenities) {
		return listing("New York", name, type, area, price, rating, slug, sleeps, amenities);
	}

	private static DbStayListing listing(
			String city,
			String name,
			StayType type,
			String area,
			int price,
			double rating,
			String slug,
			int sleeps,
			String amenities) {
		String cityKey = city.toLowerCase(Locale.ROOT);
		return new DbStayListing(
				cityKey,
				city,
				name,
				type,
				area,
				price,
				rating,
				"https://www.booking.com/hotel/sample/" + slug + ".html",
				sleeps,
				amenities);
	}
}
