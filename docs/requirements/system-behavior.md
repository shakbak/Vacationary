# System behavior

End-to-end behavior of Vacationary. The assistant, recommendation, and conversation rules are the same for every channel. Web and WhatsApp only differ at the edge.

Deployable processes: backend core; messaging platforms service (WhatsApp only in v1); web client. See [README.md](README.md).

## Actors

- **User (WhatsApp):** 1:1 chat with the bot number. Identified by WhatsApp user id.
- **User (web):** anonymous browser session. Identified by a web session id. Not the same person as a WhatsApp user unless linking is added later (it is not required).
- **Web client:** React UI. Calls the backend directly.
- **Messaging platforms service:** Separate Java service. Receives WhatsApp (and later other platforms). Calls the backend. Renders the outcome back onto the platform.
- **Assistant:** LLM that reads history, may ask for destination, may call recommendation, writes English replies. Runs in the backend.
- **Recommendation:** stay search that returns 5 lodging options with OTA links. Runs in the backend.
- **Backend core:** conversation store, isolation, idempotency, application contract, assistant, recommendation.

## Shared outcomes

Every user turn ends in exactly one of these:

| Outcome | When | User sees |
|---|---|---|
| **Recommend** | Destination is known (this message or earlier in the conversation) | English reply plus 5 lodging options (name, type, area, price/range, rating, why, OTA link) |
| **Clarify** | Destination missing or too vague | One question asking for the destination. No results. Not an error. |
| **Refuse scope** | User asked for non-lodging (flights, tours, etc.) | English refusal and an offer to help with lodging. No stay list. |
| **Fail** | LLM, search tool, or delivery/HTTP failed | What failed and what to do next (retry / rephrase). Not a hotel list. |

## Turn processing

**Web:** browser → backend (steps 2–5) → web UI renders.

**WhatsApp:** WhatsApp API → messaging platforms service → backend (steps 2–5) → messaging service renders onto WhatsApp.

1. Caller delivers to the backend: channel, external user id, text, event/message id. (Web client or messaging service.)
2. Backend ignores the turn if that event id was already processed (duplicate). Otherwise it loads that user’s conversation only.
3. Backend appends the user text and invokes the assistant with that history.
4. Assistant:
   - If destination is missing/vague → **Clarify** (no tool call).
   - If out of scope → **Refuse scope** (no tool call).
   - Else call recommendation with destination and any optional filters from this conversation → **Recommend**, or **Fail** if the tool/LLM errors.
5. Backend stores the assistant reply (except on duplicate short-circuit) and returns the outcome to the caller.
6. Web renders loading/success/error in the browser. The messaging service renders the same outcome as WhatsApp text (and later, other platforms).

## Conversation rules

- Required to recommend: destination only.
- Optional, used when present: dates, budget, guests, stay type, amenities, free-text preferences.
- If the first message is complete (“hostel in Rome under $150”) → **Recommend** in that turn. No questionnaire.
- If the first message has no destination → **Clarify** once; after the user replies with a city → **Recommend**.
- Follow-up (“cheaper”, “near the beach”) keeps the same destination and prior filters unless the user names a new city or says to start over.
- English only. Non-English may get an English “English only” reply.
- The assistant never invents stays. Names, prices, ratings, and URLs come from recommendation.

## Channel-specific behavior

**WhatsApp** (via the messaging platforms service)

- Real API, 1:1 DMs. Handled only in the messaging service.
- Render outcomes as English messages; OTA URLs must be tappable.
- Duplicate webhooks must not send a second reply.
- Delivery failure after a successful assistant run: do not spam retries; document if the user cannot be notified.

**Web**

- React + TypeScript chat with visible history.
- Loading while the turn is in flight; block send until it finishes.
- Success shows the reply (and 5 options when present). Clarification is success with a question.
- HTTP/LLM/tool failure shows the error state; user can send again.
- Refresh: restore history only if the session and in-memory store still exist; otherwise empty chat.

## Isolation

- Key: `channel` + `external user id`.
- WhatsApp user A, WhatsApp user B, and a web session are three conversations.
- No leak of history, filters, or recommendations across those keys.

## In-memory store (allowed)

- Restarting the backend wipes all conversations.
- Multiple backend instances do not share history.
- These limits must be documented.

## Assignment tests (behavior to prove)

- Two users isolated.
- Follow-up uses prior destination/context.
- LLM or tool failure produces **Fail**, not **Recommend**.
- Duplicate inbound event does not run the assistant twice or send two replies.
