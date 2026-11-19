# Messaging platforms service

A **separate Java service** for external messaging platforms (WhatsApp, and later Telegram, Slack, etc.). It is not the assistant backend. It does not own conversations, the LLM, or stay search.

**Currently implemented platform:** WhatsApp only. See [whatsapp.md](whatsapp.md). Other platforms are out of scope for v1 but this service MUST be structured so a second platform can be added without changing the backend’s assistant or recommendation logic.

The web client is **not** a messaging platform. It calls the backend directly ([web.md](web.md)).

## Must have

### Service boundary

- M-01. The messaging platforms service SHALL be a Java application, deployed as a **different process** from the backend core.
- M-02. This service SHALL be the only Java process that talks to external messaging-platform APIs (webhooks in, replies out).
- M-03. This service SHALL NOT run the LLM, SHALL NOT store canonical conversation history, and SHALL NOT search stays. Those remain in the backend ([backend.md](backend.md), [assistant.md](assistant.md), [recommendation.md](recommendation.md)).
- M-04. For every inbound user message, this service SHALL call the backend application contract (B-04–B-06) with: platform/channel id, external user id, text, and platform event/message id.
- M-05. The reply sent back on the messaging platform SHALL be a rendering of that backend outcome, not a second assistant.

### Multi-platform shape (WhatsApp only implemented)

- M-06. Platform-specific code (verify webhook, parse inbound payload, send outbound message) SHALL be isolated per platform so WhatsApp types and APIs do not leak into a shared “all platforms” core beyond a common inbound/outbound adapter contract.
- M-07. Adding a later platform (for example Telegram) SHALL mean a new adapter in **this** service plus a new `channel` value on backend calls. It SHALL NOT require changing assistant or recommendation behavior.
- M-08. v1 SHALL implement **exactly one** platform: WhatsApp. Unimplemented platforms SHALL NOT appear as half-built adapters.

### Failures and duplicates

- M-09. If the backend call fails, this service SHALL still attempt to tell the user on the originating platform that the assistant could not complete the turn and that they can retry (platform wording in [whatsapp.md](whatsapp.md)).
- M-10. Duplicate platform events SHALL NOT produce a second user-visible reply. Deduplicate with the platform event/message id; the backend also ignores duplicate ids (B-15).
- M-11. If sending the reply to the platform fails after the backend already succeeded, do not retry in a way that spams duplicates. Document the limitation if the user cannot be notified.

## Nice to have (not committed)

- A second messaging platform (Telegram, Slack, etc.).
- Channel-native UI (buttons, lists, quick replies) inside a platform adapter, with text fallback.

## Not a requirement

- Implementing any messaging platform other than WhatsApp in v1.
- Routing the **web** client through this service.
- Owning user accounts, bookings, or the lodging catalog.
- Linking a messaging-platform user to a web session.

## Acceptance

- WhatsApp traffic never needs the backend to parse WhatsApp payloads.
- The web client does not call this service.
- A WhatsApp message is forwarded to the backend contract and the backend outcome is what gets sent back on WhatsApp.
- A second platform is not implemented, but WhatsApp-specific code is confined so it is not the whole service.
