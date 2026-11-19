# Backend core

Shared Java Spring backend used by the **web client** and by the **messaging platforms service**. This service owns conversations, user isolation, the assistant, and recommendation search. It does not own messaging-platform webhooks or how a message looks on WhatsApp.

This is a **different Java process** from the messaging platforms service ([messaging.md](messaging.md)).

## Must have

### Platform

- B-01. The backend SHALL be a Java Spring application.
- B-02. The backend SHALL be the single assistant backend for all channels. The web client and the messaging platforms service SHALL call the same conversation and assistant flow, not separate bots.

### Separation of channel vs assistant

- B-03. External messaging-platform integration (parse webhook, send WhatsApp/Telegram/etc.) SHALL live in the messaging platforms service, not in this backend. The web UI SHALL live in the React client. This backend SHALL NOT depend on WhatsApp (or other platform) SDKs or payload types.
- B-04. The backend SHALL expose a channel-agnostic application contract so a new web screen or a new messaging-platform adapter can be added without changing how conversations, the LLM, or stay search work.
- B-05. That contract SHALL accept: channel identifier, external user id, inbound text, and a delivery/event id for idempotency.
- B-06. That contract SHALL return: assistant-facing text plus the structured recommendation payload defined in [recommendation.md](recommendation.md) (or a clarification/error outcome with no results).

### Conversations

- B-07. The backend SHALL support follow-up messages in the same conversation. A later message such as “cheaper” or “near the beach” SHALL be handled with prior turns available as context.
- B-08. The backend SHALL isolate conversations by channel + external user id. User A MUST NOT see or affect User B’s history, context, or recommendations.
- B-09. One WhatsApp user SHALL map to one conversation (1:1). There is no Slack-style thread model for the WhatsApp channel.
- B-10. Connecting the same person’s WhatsApp conversation to a web conversation is **not required**. Do not build account linking unless chosen later.

### Storage

- B-11. In-memory conversation storage is acceptable for the submission.
- B-12. If storage is in-memory, the project documentation SHALL state the limits: history is lost on restart, it does not work across multiple instances, and it is not durable.

### Failures (backend responsibility)

- B-13. If the LLM or the recommendation tool fails, the backend SHALL still produce a user-visible failure outcome (not a hang or empty success). Delivery onto WhatsApp is the messaging service’s job after that.
- B-14. That outcome SHALL say what happened and how to proceed (for example retry, rephrase, or try later). Platform wording on WhatsApp is specified in [whatsapp.md](whatsapp.md).
- B-15. Duplicate inbound events with the same delivery/event id SHALL NOT create a second assistant run or a second logical reply. (Required test target in the assignment.)

### Tests the backend must make possible

The assignment requires a few focused tests of meaningful behavior. The backend SHALL be testable for:

- B-16. Conversation isolation between two users.
- B-17. Follow-up context (second message uses the first).
- B-18. Failure handling when the LLM or recommendation tool fails.
- B-19. Duplicate inbound events are ignored after the first.

## Nice to have (not committed)

- Durable DB-backed conversations.
- Linking WhatsApp and web sessions for the same person.
- Progress events during a long search; cancel in-flight work.
- Multi-instance session sharing.

## Not a requirement

- User accounts, login, or passwords.
- Payments, booking completion, or storing card data.
- Admin UI, analytics, or rate-limit product features.
- Guaranteed persistence after restart (unless you later drop in-memory).
- Matching a WhatsApp user to a web user.
- Talking to the WhatsApp API (that is the messaging service).

## Acceptance

- Two different users (including two WhatsApp users, via the messaging service) can talk at once and never receive each other’s context.
- A user can send a destination, then a follow-up, and the second reply is clearly about the same trip.
- A simulated LLM or search failure yields a clear error outcome through the application contract.
- Replaying the same inbound event id does not duplicate the assistant reply.
- The web client and the messaging service can both call the same contract without WhatsApp types leaking into assistant logic.
