# Backend core

Shared Java Spring backend used by the **web client** and by the **messaging platforms service**. This service owns conversations, user isolation, the assistant, and recommendation search. It does not own messaging-platform webhooks or how a message looks on WhatsApp.

This is a **different Java process** from the messaging platforms service ([messaging.md](messaging.md)). Assistant and recommendation run **inside this process**, not as their own services.

## Must have

### Platform and contract

- B-01. The backend SHALL be a Java Spring application.
- B-02. The backend SHALL be the single assistant backend for all channels. The web client and the messaging platforms service SHALL call the same conversation and assistant flow, not separate bots.
- B-03. External messaging-platform integration (parse webhook, send WhatsApp/Telegram/etc.) SHALL live in the messaging platforms service, not in this backend. The web UI SHALL live in the React client. This backend SHALL NOT depend on WhatsApp (or other platform) SDKs or payload types.
- B-04. The backend SHALL expose a channel-agnostic application contract so a new web screen or a new messaging-platform adapter can be added without changing how conversations, the LLM, or stay search work.
- B-05. That contract SHALL accept: channel identifier, external user id, inbound text, and a delivery/event id for idempotency.
- B-06. That contract SHALL return: assistant-facing text plus the structured recommendation payload defined under Recommendation below (or a clarification/error outcome with no results).

### Conversations

- B-07. The backend SHALL support follow-up messages in the same conversation. A later message such as “cheaper” or “near the beach” SHALL be handled with prior turns available as context.
- B-08. The backend SHALL isolate conversations by channel + external user id. User A MUST NOT see or affect User B’s history, context, or recommendations.
- B-09. One WhatsApp user SHALL map to one conversation (1:1). There is no Slack-style thread model for the WhatsApp channel.
- B-10. Connecting the same person’s WhatsApp conversation to a web conversation is **not required**. Do not build account linking unless chosen later.

### Storage

- B-11. In-memory conversation storage is acceptable for the submission.
- B-12. If storage is in-memory, the project documentation SHALL state the limits: history is lost on restart, it does not work across multiple instances, and it is not durable.

### Failures and tests

- B-13. If the LLM or the recommendation tool fails, the backend SHALL still produce a user-visible failure outcome (not a hang or empty success). Delivery onto WhatsApp is the messaging service’s job after that.
- B-14. That outcome SHALL say what happened and how to proceed (for example retry, rephrase, or try later). Platform wording on WhatsApp is specified in [messaging.md](messaging.md).
- B-15. Duplicate inbound events with the same delivery/event id SHALL NOT create a second assistant run or a second logical reply.
- B-16. The backend SHALL be testable for conversation isolation between two users.
- B-17. The backend SHALL be testable for follow-up context (second message uses the first).
- B-18. The backend SHALL be testable for failure handling when the LLM or recommendation tool fails.
- B-19. The backend SHALL be testable that duplicate inbound events are ignored after the first.

### Assistant

LLM-backed assistant that understands English lodging requests, keeps conversation context, calls the recommendation tool, and produces a reply the channels can deliver.

- A-01. The assistant SHALL use an LLM to understand the user and to compose the reply.
- A-02. The assistant SHALL have **at least one tool**: lodging search, as specified under Recommendation below.
- A-03. A simple single-agent loop is sufficient. Multiple agents and advanced orchestration are not required.
- A-04. The assistant SHALL be channel-agnostic. It consumes conversation history + latest user text and produces a structured outcome. It SHALL NOT format WhatsApp (or other messaging-platform) protocol payloads. Those belong in the messaging platforms service.
- A-05. The assistant SHALL read and write **English only**. Non-English input is outside the product; it may reply in English that it only supports English.
- A-06. If the user includes a destination in one message (with or without extras), the assistant SHALL search and recommend in that turn. It SHALL NOT ask a full questionnaire first.
- A-07. If destination is missing or too vague to search (“somewhere warm”, “a hotel”), the assistant SHALL ask **one** clarifying question for the destination and wait. It SHALL NOT ask dates, budget, or number of people in that same message as a requirement.
- A-08. Dates, budget, **number of people** (total guest count), stay type, and amenities are optional. If the user provided them, the assistant SHALL pass them to search. If not, the assistant SHALL still search once destination is known. Number of people is a first-class filter (for example “for 4”), not a buried note.
- A-09. Clarifying questions SHALL be asked **one at a time**. For v1 the only blocking question is destination. Optional extras are not a gate.
- A-10. Follow-up messages SHALL reuse destination and optional filters **already stated in prior messages of the same conversation**, unless the user clearly starts a new search (new city or “start over”). Do not keep a separate destination/filter snapshot beside the message list.
- A-11. After a successful search, the assistant outcome SHALL include the 5 structured options from the tool **and** English text any channel can show, including the OTA links.
- A-12. The assistant SHALL NOT invent stays that the tool did not return. Names, prices, ratings, and URLs in the reply SHALL come from the tool payload.
- A-13. If the user asks for non-lodging help (flights, tours), the assistant SHALL refuse that scope and offer lodging help instead.
- A-14. If the LLM call fails, the outcome SHALL be a clear failure: the assistant could not process the message; the user can retry.
- A-15. If the recommendation tool fails, the outcome SHALL be a clear failure: search did not work; the user can retry or rephrase. This is different from “need a destination”.
- A-16. The assistant SHALL NOT present a failure as if 5 hotels were found.
- A-17. Follow-up context: after “hotels in Lisbon”, “cheaper ones” SHALL still be about Lisbon.
- A-18. Isolation is a backend property; the assistant must not mix another user’s history into the prompt.

### Recommendation

Stay-search logic that turns a request into five lodging options with OTA links. The assistant calls this as its required tool. Local sample data is allowed.

- R-01. Search SHALL recommend **lodging only**: hotel, motel, vacation rental, hostel, B&B, resort.
- R-02. Search SHALL NOT recommend flights, tours, attractions, restaurants, or other non-lodging services.
- R-03. Search SHALL accept **any city name** as destination. Unknown or uncovered cities SHALL still return five sample options for that city, with sample OTA links if needed, rather than refusing the city.
- R-04. **Required input:** destination (city or equivalent place name). Without a destination the tool SHALL NOT be considered successfully completed; the assistant must ask for destination instead.
- R-05. **Optional input, used when present:** dates, budget, **number of people** (total guest count), stay type (from R-01), amenities, and other free-text preferences (for example “near the beach”, “quiet”, “family”).
- R-05a. When number of people is present, the 5 options SHALL prefer stays that can sleep that party, as far as the data allows. Mention party size in `why` when it affected the match.
- R-06. Optional fields SHALL NOT be required to produce results. Missing dates, budget, or number of people is a valid search.
- R-07. A successful search SHALL return **exactly 5** lodging options for a typical recommendation turn.
- R-08. Each option SHALL include: name; stay type (one of R-01); location/area (at least city; neighborhood or area when known); price or price range; rating; short why; OTA listing URL (Booking.com, Airbnb, or similar; **real or sample**).
- R-09. Links SHALL be OTA listing links, not only a generic web search or a map query. Sample/fake OTA URLs are allowed.
- R-10. Results SHALL be in English (names may stay as commonly written for that property).
- R-11. When optional filters are present, the 5 options SHALL reflect them as far as the data allows (for example budget-aware prices, requested stay type). If the catalog cannot fully match, still return 5 options and say so in each option’s `why` or in a search-level note the assistant can speak.
- R-12. The tool SHALL be usable again in the same conversation with updated criteria (cheaper, different area, different stay type) so follow-ups can return a new set of 5 options.
- R-13. A local sample catalog and simulated listings are allowed and sufficient.
- R-14. Sample listings used for arbitrary cities SHALL still look like real recommendations: distinct names, types, areas, prices, ratings, reasons, and OTA-style URLs. Do not return empty or “lorem ipsum” placeholders as the user-visible result.
- R-15. If search itself fails (tool error, not “city unknown”), it SHALL surface a failure the backend can turn into a user-visible “search failed, try again” outcome. “City unknown” is **not** a failure; see R-03.

## Nice to have (not committed)

- Durable DB-backed conversations.
- Linking WhatsApp and web sessions for the same person.
- Progress events during a long search; cancel in-flight work; streaming the reply.
- Multi-instance session sharing.
- Buttons / quick replies as input (channel concern; assistant still sees text or an equivalent intent).
- Preview-and-confirm before an action (no booking action in v1).
- Multiple tools (weather, compare two properties).
- Live Booking.com / Airbnb / Google Places APIs.
- Photos, maps, availability, or real-time prices.
- Ranking beyond “fits the request”.
- Returning more or fewer than 5 because the user asked “just one” / “show more”.
- Special stay requests: room type (double, twin, king, suite, family room, dorm bed, and similar) and occupancy split (how many people in each room, for example 2+2 or a double plus a single). Adults vs children, if collected, is part of this enhancement, not v1 must-have.

## Not a requirement

- User accounts, login, or passwords.
- Payments, booking completion, commissions, or storing card data.
- Admin UI, analytics, or rate-limit product features.
- Guaranteed persistence after restart (unless you later drop in-memory).
- Matching a WhatsApp user to a web user.
- Talking to the WhatsApp API (that is the messaging service).
- Multilingual replies or a full intake form before every search.
- Multi-agent orchestration.
- Covering every hotel in a city, or guaranteeing sample OTA links are live bookable pages.

## Acceptance

- Two different users (including two WhatsApp users, via the messaging service) can talk at once and never receive each other’s context.
- “Find me a hotel in Rome” → 5 lodging options with links, no extra required questions.
- “I need a place to stay” → one question: where? After “Rome” → 5 options.
- “Rome, 12–15 March, under $150, hostel” → 5 options using those filters.
- “Rome for 4 people” → 5 options that prefer capacity for 4; `why` can mention the party size. Missing a people count still returns 5 options.
- “Show cheaper ones” after a Rome search → new 5 options still for Rome.
- Given a city with no curated data, the user still gets 5 sample lodging options for that city, not an error.
- LLM or tool outage → English error that says what failed and to retry, not a fake list.
- Replaying the same inbound event id does not duplicate the assistant reply.
- The web client and the messaging service can both call the same contract without WhatsApp types leaking into assistant logic.
