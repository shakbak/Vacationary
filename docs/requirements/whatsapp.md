# WhatsApp platform (inside the messaging service)

WhatsApp is the only messaging platform implemented in v1. It lives in the **messaging platforms service** ([messaging.md](messaging.md)), not in the backend and not as its own extra Java process.

Users send a message on WhatsApp and receive the shared assistant’s reply through WhatsApp’s actual API.

## Must have

### Real integration

- W-01. Users SHALL send messages and receive replies through a **real** WhatsApp API (not a mocked inbox in the app).
- W-02. Conversation model: **1:1 direct chats** between the user and the business/bot number. There are no group-chat or Slack-thread requirements.
- W-03. The submission SHALL explain this choice: travel help is naturally a private 1:1 chat; WhatsApp maps one phone number to one user; it matches how people already message.

### Mapping onto the backend (via the messaging service)

- W-04. Inbound WhatsApp text SHALL be turned into a backend call with: channel = WhatsApp, external user id = the user’s WhatsApp identifier, message text, and the platform event/message id.
- W-05. The reply sent to WhatsApp SHALL come from the backend/assistant outcome, not from a second bot.
- W-06. Structured recommendation results SHALL be rendered as English WhatsApp text that includes all required fields for each of the 5 options: name, stay type, location/area, price or price range, rating, short why, and the OTA URL (tappable link).
- W-07. Clarification questions (need a destination) SHALL be sent as a normal English WhatsApp text message.
- W-08. Failure outcomes SHALL be sent as English WhatsApp text that states what happened and how to proceed.

### Reliability

- W-09. Duplicate WhatsApp webhook/event deliveries SHALL NOT produce duplicate replies. Use the platform message/event id with messaging-service and backend idempotency (M-10, B-15).
- W-10. If WhatsApp **delivery** fails after the assistant already ran, the user-visible behavior SHALL still be a failure indication if the user can be reached; if they cannot, document the limitation. Do not retry in a way that spams duplicates.

### Demo

- W-11. Reviewers SHALL be able to talk to the WhatsApp assistant during the review period (working number or join instructions). A locally running messaging service is acceptable if they can reach it remotely.
- W-12. Access instructions for the WhatsApp bot SHALL be in the repository (how to start a chat, any test number, that English is required).

## Nice to have (not committed)

From the assignment; pick at most one or two later, fully finished:

- Channel-native interactions: buttons, list messages, or quick replies, with a useful **text fallback** (required if this is implemented, because WhatsApp clients differ).
- Progress while searching; cancel.
- Handling a second message while a reply is still being produced.
- Proactive later notifications (WhatsApp template/session rules would apply; not required).

## Not a requirement

- WhatsApp groups, broadcasts, or multi-party chats.
- Official WhatsApp conversation windows / template-message product features beyond whatever the chosen API needs to reply in-session.
- Linking the WhatsApp user to a web login.
- Media input (images, voice notes, location pins) as a way to search. Text is enough.
- Hebrew or any language other than English on this channel.
- A second messaging platform in v1.

## Acceptance

- A reviewer sends an English WhatsApp message to the bot and gets a real WhatsApp reply from the shared assistant.
- “Hotels in Barcelona” yields one WhatsApp message (or a short sequence) listing 5 stays, each with type, area, price/range, rating, why, and an OTA link.
- A message with no destination gets a single question asking where, not five fake hotels.
- The same inbound webhook delivered twice results in one reply.
- A tool/LLM failure is visible on WhatsApp as an error with a next step, not silence.
- WhatsApp webhook handling runs in the messaging platforms service, not in the backend core.
