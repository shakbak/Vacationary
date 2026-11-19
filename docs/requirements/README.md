# Vacationary requirements

Requirements for the Channels Challenge, split by service. These describe **what** must be true before design and implementation. They do not prescribe Spring packages, WhatsApp vendors, LLM providers, or data stores.

## Product (locked)

A vacation **lodging** recommendation assistant.

- **In scope:** hotels, motels, vacation rentals, hostels, B&Bs, resorts.
- **Out of scope:** flights, tours, attractions, restaurants, and any non-lodging booking.
- **Coverage:** any city the user names. Sample/fake OTA links are allowed when live listings are unavailable.
- **Required user input:** destination only. Dates, budget, guests, amenities, and stay type are optional and used when present.
- **Conversation:** hybrid. Accept a full request in one message. If the destination is missing, ask for it (one question at a time). Do not block on optional fields.
- **Language:** English only.
- **Results:** 5 options per typical reply. Each option has name, stay type, location/area, price or price range, rating, short why, and an OTA listing link (Booking.com, Airbnb, or similar; real or sample URL).
- **Channels:** WhatsApp 1:1 chats and a React/TypeScript web chat. Same assistant; conversations are not linked across channels.

## Deployable pieces (locked)

| Piece | Kind | Role |
|---|---|---|
| Backend core | Java Spring process | Conversations, assistant, recommendation. HTTP contract for callers. |
| Messaging platforms service | Separate Java process | External messaging platforms. WhatsApp only in v1. Calls the backend; never owns the LLM or catalog. |
| Web client | React/TypeScript | Browser chat. Calls the backend directly, not the messaging service. |

Assistant and recommendation are **not** their own processes; they live in the backend core.

## Spec files

| File | Part | Role |
|---|---|---|
| [system-behavior.md](system-behavior.md) | Whole system | Shared outcomes and turn flow for every channel |
| [backend.md](backend.md) | Backend core | Shared API, conversations, isolation, assistant + search |
| [recommendation.md](recommendation.md) | Recommendation | Stay search and 5-result payload with OTA links (in the backend) |
| [assistant.md](assistant.md) | Assistant | LLM understands the user, fills destination, calls recommendation (in the backend) |
| [messaging.md](messaging.md) | Messaging platforms service | Separate Java service for external messaging APIs |
| [whatsapp.md](whatsapp.md) | WhatsApp adapter | Only implemented platform, inside the messaging service |
| [web.md](web.md) | Web client | React/TypeScript chat UI for the same assistant |

## How to read a file

- **Must have** — required for a valid submission or for locked product choices.
- **Nice to have** — optional in the assignment. Not committed.
- **Not a requirement** — omitted by the assignment, therefore out of scope unless later chosen as a product feature.

## Global assignment constraints (all services)

- A working core plus documented gaps beats unfinished extras.
- At most one or two nice-to-haves, fully done, if any.
- In-memory storage is allowed; limitations must be documented.
- Paid services are not required.
- Local sample data and simulated actions are allowed.
- AI coding tools are allowed; submitted code must be understandable and changeable by you.
