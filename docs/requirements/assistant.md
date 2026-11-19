# Assistant

LLM-backed assistant that understands English lodging requests, keeps conversation context, calls the recommendation tool, and produces a reply the channels can deliver.

## Must have

### Role

- A-01. The assistant SHALL use an LLM to understand the user and to compose the reply.
- A-02. The assistant SHALL have **at least one tool**: lodging search, as specified in [recommendation.md](recommendation.md).
- A-03. A simple single-agent loop is sufficient. Multiple agents and advanced orchestration are not required.
- A-04. The assistant SHALL be channel-agnostic. It consumes conversation history + latest user text and produces a structured outcome. It SHALL NOT format WhatsApp protocol payloads.

### Language

- A-05. The assistant SHALL read and write **English only**. Non-English input is outside the product; it may reply in English that it only supports English.

### Conversation: hybrid, destination required

- A-06. If the user includes a destination in one message (with or without extras), the assistant SHALL search and recommend in that turn. It SHALL NOT ask a full questionnaire first.
- A-07. If destination is missing or too vague to search (“somewhere warm”, “a hotel”), the assistant SHALL ask **one** clarifying question for the destination and wait. It SHALL NOT ask dates, budget, or guests in that same message as a requirement.
- A-08. Dates, budget, guests, stay type, and amenities are optional. If the user provided them, the assistant SHALL pass them to search. If not, the assistant SHALL still search once destination is known.
- A-09. Clarifying questions SHALL be asked **one at a time**. For v1 the only blocking question is destination. Optional extras are not a gate.
- A-10. Follow-up messages SHALL reuse trip context already collected (destination and any optional filters) unless the user clearly starts a new search (new city or “start over”).

### Using recommendations

- A-11. After a successful search, the assistant outcome SHALL include the 5 structured options from the tool **and** English text the user can read on WhatsApp, including the OTA links.
- A-12. The assistant SHALL NOT invent stays that the tool did not return. Names, prices, ratings, and URLs in the reply SHALL come from the tool payload.
- A-13. If the user asks for non-lodging help (flights, tours), the assistant SHALL refuse that scope and offer lodging help instead.

### Failures

- A-14. If the LLM call fails, the outcome SHALL be a clear failure: the assistant could not process the message; the user can retry.
- A-15. If the recommendation tool fails, the outcome SHALL be a clear failure: search did not work; the user can retry or rephrase. This is different from “need a destination”.
- A-16. The assistant SHALL NOT present a failure as if 5 hotels were found.

### Tests (assignment)

- A-17. Follow-up context: after “hotels in Lisbon”, “cheaper ones” SHALL still be about Lisbon.
- A-18. Isolation is a backend property; the assistant must not mix another user’s history into the prompt.

## Nice to have (not committed)

- Buttons / quick replies as the source of the next user message (channel concern; assistant still sees text or an equivalent intent).
- Preview-and-confirm before an action (no booking action in v1).
- User interrupts while a search is running; cancel; progress tokens.
- Streaming the reply.
- Multiple tools (weather, compare two properties).

## Not a requirement

- Multilingual replies.
- Collecting a complete intake form before every search.
- Account linking across channels.
- Completing bookings or payments.
- Multi-agent orchestration.

## Acceptance

- “Find me a hotel in Rome” → 5 lodging options with links, no extra required questions.
- “I need a place to stay” → one question: where? After “Rome” → 5 options.
- “Rome, 12–15 March, under $150, hostel” → 5 options using those filters, no extra questions.
- “Show cheaper ones” after a Rome search → new 5 options still for Rome.
- LLM or tool outage → English error that says what failed and to retry, not a fake list.
