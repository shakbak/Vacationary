# Recommendation domain — inner design

Stay search. No HTTP, no LLM, no conversations. **Manager + repository + mapper + in-memory data**, plus a provider so “any city” works.

Implement this first.

## Inner parts

```
recommendation
  StayManager                implements IStayManager
        │
        ├── IStayRepository  repository: curated listings
        │     └── mapper     DbStayListing → BlStayOption
        └── IStayProvider    infra: 5 sample options for any city
```

| Part | Responsibility |
|---|---|
| `IStayManager` | Port the assistant calls: `search(query)` |
| `StayManager` | Destination required, apply filters, always 5 options; unknown city is not failure |
| `IStayRepository` | `findByDestination(city) → rows` |
| `InMemoryStayRepository` | Fixture list in a `Map` / list; v1 persistence |
| `DbStayListing` | Persistence shape (capacity, raw price, city key, …) |
| `StayListingMapper` | `DbStayListing` → `BlStayOption` (fills `why` using the query) |
| `IStayProvider` | `generate(query) → 5 BlStayOption` when repository is thin or city unknown |
| `BlStaySearchQuery` / `BlStayOption` | Types this domain and the assistant share for the tool |
| `BlStaySearchException` | Internal search failure (not “unknown city”) |

`StaySearchResult` is either **success (exactly 5 `BlStayOption`)** or **failure** (BL error). Repository miss is not a failure.

## Types

**`StayType`:** `hotel` | `motel` | `vacation_rental` | `hostel` | `bnb` | `resort`

**`BlStaySearchQuery`:** `destination` (required), optional `dates`, `budget`, `guestCount`, `stayType`, `preferences`.

**`BlStayOption`:** `name`, `stayType`, `location`, `price`, `rating`, `why`, `url`.

**`DbStayListing`:** at least city key, name, type, area, price, rating, url, **sleeps** (for `guestCount`). Not an HTTP DTO.

## How the parts interact

```
StayManager.search(BlStaySearchQuery)
  1. If destination blank → BL validation error
       (assistant must not call; this is a programmer error)
  2. rows = IStayRepository.findByDestination(destination)
  3. If rows empty:
       options = IStayProvider.generate(query)     // any city
     Else:
       options = rows mapped through StayListingMapper.toBl(row, query)
       options = filter/prefer stayType, budget, guestCount, preferences
       if options.size < 5:
         options += IStayProvider.generate(query) until 5
       if a filter did not fully match: mapper/manager puts that in why
  4. return success(options[0..4])
  5. Unexpected exception → BlStaySearchException (failure)
```

`StayListingMapper.toBl` writes `why` from the query (e.g. “Sleeps 4 in Trastevere”). The provider must produce the same `BlStayOption` shape: distinct names, real-looking OTA URLs, no “Hotel 1” / lorem.

The manager is **stateless** besides its two collaborators. A follow-up search is a new `search` call with a new query (the assistant puts destination and filters on that query from the chat history).

## Wiring

```
IStayManager ← StayManager(IStayRepository, IStayProvider, StayListingMapper)
IStayRepository ← InMemoryStayRepository(fixture rows)
IStayProvider ← SampleStayProvider
```

Tests can bind a fake `IStayManager` (assistant tests) or a fake `IStayRepository` (manager tests).

## Swap

- Replace `InMemoryStayRepository` with another `IStayRepository` without touching the manager.
- Replace `IStayManager` entirely (live OTA) without touching the assistant.

## Out of this domain

- HTTP, WhatsApp, LLM
- Asking for destination
- Room type / occupancy-split fields (nice to have)
- Photos, live availability
