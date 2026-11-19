# Recommendation

Stay-search logic that turns a request into five lodging options with OTA links. The assistant calls this as its required tool. It runs **inside the backend core**, not as its own process. Local sample data is allowed.

## Must have

### Scope

- R-01. The service SHALL recommend **lodging only**: hotel, motel, vacation rental, hostel, B&B, resort.
- R-02. The service SHALL NOT recommend flights, tours, attractions, restaurants, or other non-lodging services.
- R-03. The service SHALL accept **any city name** as destination. Unknown or uncovered cities SHALL still return five sample options for that city, with sample OTA links if needed, rather than refusing the city.

### Input

- R-04. **Required input:** destination (city or equivalent place name). Without a destination the tool SHALL NOT be considered successfully completed; the assistant must ask for destination instead (see [assistant.md](assistant.md)).
- R-05. **Optional input, used when present:** dates, budget, guest count, stay type (from R-01), amenities, and other free-text preferences (for example “near the beach”, “quiet”, “family”).
- R-06. Optional fields SHALL NOT be required to produce results. Missing dates, budget, or guests is a valid search.

### Output

- R-07. A successful search SHALL return **exactly 5** lodging options for a typical recommendation turn.
- R-08. Each option SHALL include all of the following:
  - name
  - stay type (one of R-01)
  - location/area (at least city; neighborhood or area when known)
  - price or price range
  - rating
  - short why (why this fits the user’s request)
  - OTA listing URL (Booking.com, Airbnb, or similar; **real or sample**)
- R-09. Links SHALL be OTA listing links, not only a generic web search or a map query. Sample/fake OTA URLs are allowed.
- R-10. Results SHALL be in English (names may stay as commonly written for that property).
- R-11. When optional filters are present, the 5 options SHALL reflect them as far as the data allows (for example budget-aware prices, requested stay type). If the catalog cannot fully match, still return 5 options and say so in each option’s `why` or in a search-level note the assistant can speak.

### Follow-up searches

- R-12. The tool SHALL be usable again in the same conversation with updated criteria (cheaper, different area, different stay type) so follow-ups can return a new set of 5 options.

### Data

- R-13. A local sample catalog and simulated listings are allowed and sufficient.
- R-14. Sample listings used for arbitrary cities SHALL still look like real recommendations: distinct names, types, areas, prices, ratings, reasons, and OTA-style URLs. Do not return empty or “lorem ipsum” placeholders as the user-visible result.

### Failures

- R-15. If search itself fails (tool error, not “city unknown”), the service SHALL surface a failure the backend can turn into a user-visible “search failed, try again” outcome. “City unknown” is **not** a failure; see R-03.

## Nice to have (not committed)

- Live Booking.com / Airbnb / Google Places APIs.
- Photos, maps, availability, or real-time prices.
- Ranking beyond “fits the request”.
- Returning more or fewer than 5 because the user asked “just one” / “show more” (not required for v1; typical reply is 5).
- Completing a booking inside the product.

## Not a requirement

- Charging the user or taking a commission.
- Guaranteeing that a sample OTA link is a live bookable page.
- Covering every hotel in a city.
- Non-lodging vacation services.

## Acceptance

- Given only “Paris”, the tool returns 5 lodging options, each with type, area, price or range, rating, why, and an OTA URL.
- Given “Paris” plus “hostel” and a budget, the 5 options are hostels (or the `why` explains a fallback) and prices relate to the budget.
- Given a city with no curated data, the user still gets 5 sample lodging options for that city, not an error.
- A request for tours or flights is outside this tool; the assistant must not present them as stay results.
