# Messaging platforms service

A **separate Java service** for external messaging platforms (Telegram in v1, and later another platform if needed). It is not the assistant backend. It does not own conversations, the LLM, or stay search.

**Currently implemented platform:** Telegram only (this file). Other platforms are out of scope for v1 but this service MUST be structured so a second platform can be added without changing the backend’s assistant or recommendation logic.

v1 is Telegram: BotFather is enough to go live, and long polling does not need a public HTTPS URL.

The web client is **not** a messaging platform. It calls the backend directly ([web.md](web.md)).

## Must have

### Service boundary

- M-01. The messaging platforms service SHALL be a Java application, deployed as a **different process** from the backend core.
- M-02. This service SHALL be the only Java process that talks to external messaging-platform APIs (inbound updates in, replies out).
- M-03. This service SHALL NOT run the LLM, SHALL NOT store canonical conversation history, and SHALL NOT search stays. Those remain in the backend ([backend.md](backend.md)).
- M-04. For every inbound user message, this service SHALL call the backend application contract (B-04–B-06) with: platform/channel id, external user id, text, and platform event/message id.
- M-05. The reply sent back on the messaging platform SHALL be a rendering of that backend outcome, not a second assistant.
- M-06. Platform-specific code (receive updates, parse inbound payload, send outbound message) SHALL be isolated per platform so Telegram types and APIs do not leak into a shared “all platforms” core beyond a common inbound/outbound adapter contract.
- M-07. Adding a later platform SHALL mean a new adapter in **this** service plus a new `channel` value on backend calls. It SHALL NOT require changing assistant or recommendation behavior.
- M-08. v1 SHALL implement **exactly one** platform: Telegram. Unimplemented platforms SHALL NOT appear as half-built adapters.
- M-09. If the backend call fails, this service SHALL still attempt to tell the user on the originating platform that the assistant could not complete the turn and that they can retry (Telegram wording below).
- M-10. Duplicate platform events SHALL NOT produce a second user-visible reply. Deduplicate with the platform event/message id; the backend also ignores duplicate ids (B-15).
- M-11. If sending the reply to the platform fails after the backend already succeeded, do not retry in a way that spams duplicates. Document the limitation if the user cannot be notified.

### Telegram (only implemented platform)

Telegram lives in this service, not in the backend and not as its own extra Java process. Users send a message to the bot and receive the shared assistant’s reply through Telegram’s actual Bot API.

- T-01. Users SHALL send messages and receive replies through the **real** Telegram Bot API (not a mocked inbox in the app).
- T-02. Conversation model: **1:1 direct chats** between the user and the bot. Groups, channels, and Slack-style threads are not required.
- T-03. The submission SHALL explain this choice: travel help is naturally a private 1:1 chat; Telegram maps one user id to one user; BotFather is enough to go live.
- T-04. Inbound Telegram text SHALL be turned into a backend call with: channel = telegram, external user id = the user’s Telegram id, message text, and the platform event id (`update_id`).
- T-05. The reply sent to Telegram SHALL come from the backend/assistant outcome, not from a second bot.
- T-06. Structured recommendation results SHALL be rendered as English Telegram text that includes all required fields for each of the 5 options: name, stay type, location/area, price or price range, rating, short why, and the OTA URL (tappable link).
- T-07. Clarification questions (need a destination) SHALL be sent as a normal English Telegram text message.
- T-08. Failure outcomes SHALL be sent as English Telegram text that states what happened and how to proceed.
- T-09. Duplicate Telegram updates SHALL NOT produce duplicate replies. Use `update_id` with messaging-service and backend idempotency (M-10, B-15).
- T-10. If Telegram **delivery** fails after the assistant already ran, the user-visible behavior SHALL still be a failure indication if the user can be reached; if they cannot, document the limitation. Do not retry in a way that spams duplicates.
- T-11. Reviewers SHALL be able to talk to the Telegram bot during the review period (bot username / `t.me` link). A locally running messaging service is acceptable; long polling does not require a public HTTPS URL.
- T-12. Access instructions for the Telegram bot SHALL be in the repository (how to start a chat, bot username, that English is required).
- T-13. v1 MAY receive updates by **long polling** (`getUpdates`) or by webhook. Long polling is sufficient. Only private-chat text is in scope; ignore groups, media, and non-text updates.

## Nice to have (not committed)

From the assignment; pick at most one or two later, fully finished:

- A second messaging platform.
- Channel-native interactions: buttons, list messages, or quick replies, with a useful **text fallback** (required if this is implemented).
- Progress while searching; cancel.
- Handling a second message while a reply is still being produced.
- Proactive later notifications.

## Not a requirement

- Implementing any messaging platform other than Telegram in v1.
- Routing the **web** client through this service.
- Owning user accounts, bookings, or the lodging catalog.
- Linking a messaging-platform user to a web session.
- Telegram groups, channels, or multi-party chats.
- Inline keyboards, commands beyond treating `/start` as inbound text, stickers, or voice.
- Media input (images, voice notes, location pins) as a way to search. Text is enough.
- Hebrew or any language other than English on this channel.

## Acceptance

- Telegram traffic never needs the backend to parse Telegram payloads.
- The web client does not call this service.
- A reviewer sends an English Telegram message to the bot and gets a real Telegram reply from the shared assistant.
- “Hotels in Barcelona” yields one Telegram message (or a short sequence) listing 5 stays, each with type, area, price/range, rating, why, and an OTA link.
- A message with no destination gets a single question asking where, not five fake hotels.
- The same inbound update delivered twice results in one reply.
- A tool/LLM failure is visible on Telegram as an error with a next step, not silence.
- A second platform is not implemented, but Telegram-specific code is confined so it is not the whole service.
