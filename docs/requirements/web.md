# Web client

Lightweight React and TypeScript chat UI. Users message the **same** assistant as on Telegram, through the shared backend. This client renders conversations; it does not search stays or call the LLM.

Product behavior matches the assistant and recommendation rules in [backend.md](backend.md): English, destination required, optional extras, five lodging results with OTA links.

## Must have

### Platform and scope

- WEB-01. The web client SHALL be a React application written in TypeScript.
- WEB-02. The interface SHALL be small and usable. Usability matters more than visual complexity.
- WEB-03. The client SHALL talk to the shared backend contract (B-04–B-06) **directly**. It SHALL NOT call the messaging platforms service, implement a second assistant, a second conversation store, or its own stay search.
- WEB-04. A web conversation SHALL use channel = web and an external user id that identifies this browser session. It SHALL NOT use a Telegram id.
- WEB-05. Connecting a web user to a Telegram user is **not required**. History is not shared across those channels.

### Conversation UI

- WEB-06. The user SHALL be able to type an English message and send it to the assistant.
- WEB-07. The client SHALL show the conversation history for this session: prior user messages and assistant replies, in order, on the same screen as the composer.
- WEB-08. After a successful turn, the new user message and the assistant reply SHALL appear in that history without a full page reload.
- WEB-09. Follow-up messages (for example “cheaper”, “near the beach”, or “we’re 4 people”) SHALL be sent as ordinary chat messages on the same conversation so prior turns (destination, people count, and other filters) are still in the history. Number of people is not a separate form field in v1.
- WEB-10. No login, signup, or password screen. An anonymous session is enough.

### Loading, success, and error

- WEB-11. While waiting for the backend, the UI SHALL show a **loading** state (the user can tell the request is in progress).
- WEB-12. The user SHALL NOT be able to send another message during loading (prevents double submit of the same turn).
- WEB-13. **Success:** the assistant reply is shown. If the outcome includes recommendations, all 5 options SHALL be visible.
- WEB-14. Each shown option SHALL include: name, stay type, location/area, price or price range, rating, short why, and a clickable OTA URL.
- WEB-15. **Clarification** (destination missing) is a successful assistant turn, not an error: show the question in the transcript and wait for the next user message.
- WEB-16. **Error:** if the LLM, recommendation tool, or HTTP call fails, the UI SHALL show an error state that states what happened and how to proceed (retry or rephrase). It SHALL NOT look like a successful list of hotels.
- WEB-17. After an error, the user SHALL be able to send another message (retry).

### Usability

Usability is a must-have (WEB-02, assignment item 2). These rules keep the small UI easy to scan and recover from. They do **not** add photos, cancel, extra forms, or a new-conversation button.

- WEB-23. When the transcript is empty, the UI SHALL show a short English empty state: this is a lodging assistant; a city is enough to start. It SHALL NOT be a form, a questionnaire, or an error.
- WEB-24. On HTTP or network failure (turn not saved), the composer SHALL keep the unsent text so the user can retry without retyping. On HTTP 200 (including `fail`), the composer SHALL clear as after a normal send.
- WEB-25. After a send attempt finishes (success or error), keyboard focus SHALL return to the composer so a follow-up is one keystroke away.
- WEB-26. The transcript SHALL scroll so the latest user message, loading indicator, assistant reply, or stay cards are in view. The composer SHALL stay visible; it SHALL NOT cover that latest content.
- WEB-27. The layout SHALL work at a typical laptop width and at a narrow phone-width viewport (~360px): one column, transcript scrolls, composer pinned at the bottom.
- WEB-28. Loading SHALL be visible **in the transcript** (pending user message plus an in-progress assistant indicator and short copy such as “Finding stays…”). A disabled send button alone is not enough.
- WEB-29. **Clarify** and **refuse scope** SHALL look like ordinary assistant messages. **`fail` (HTTP 200)** SHALL appear in the transcript as an assistant error (what happened and what to do). **HTTP/network error** SHALL appear as status near the composer (the turn was not saved), not as a fake assistant hotel reply. The three SHALL be distinguishable without relying on color alone (WEB-30).
- WEB-30. Loading, success, and error SHALL be stated in English text, not color or icons alone.
- WEB-31. Stay cards SHALL be easy to scan: property **name** most prominent; stay type as ordinary words (Hotel, vacation rental, B&B — not raw enums); location, price, rating, and why readable; OTA URL a clear **View listing** link (or equivalent), opening in a new tab. All five cards visible without a carousel.
- WEB-32. The composer SHALL have an accessible name (visible label or `aria-label`). Enter sends. While a turn is in flight, both field and button are disabled and the loading copy explains the wait (`aria-busy` or equivalent).
- WEB-33. An error SHALL stay visible until the user starts the next send (or it is replaced by a new result). It SHALL NOT disappear on its own.
- WEB-34. If history cannot be loaded (backend restarted or the GET fails), the UI SHALL still start empty with the composer enabled (WEB-21). It MAY show a short non-blocking note that earlier messages could not be loaded. It SHALL NOT block chat behind a full-page error.

### Language and product rules (same assistant)

- WEB-18. UI copy and chat SHALL be English only.
- WEB-19. The client SHALL display whatever outcome the assistant returns (recommendations, one destination question, out-of-scope refusal, or failure). It SHALL NOT add extra intake forms, extra required fields, or extra hotels.

### Isolation and session

- WEB-20. Two different browser sessions SHALL not see each other’s history (backend isolation, B-08).
- WEB-21. Refreshing the page MAY restore history only if the same session id is still known to the client **and** the in-memory backend still has that conversation. If the backend was restarted, the UI SHALL start empty rather than fail. Document this with the in-memory limitation (B-12).

### Demo

- WEB-22. Reviewers SHALL be able to open a working web URL during the review period and complete a chat (destination → 5 results, a follow-up, and a failure path as in the demo video).

## Nice to have (not committed)

- Result cards with photos.
- Progress during a long search; a cancel control.
- Starting on Telegram and continuing on the web (account/session linking).
- Handling a second typed message while a reply is still loading (v1 forbids send during load instead).
- A dedicated “new conversation” button (the user can already say “start over” in chat).
- UI for room type or occupancy split (v1: user can type it; backend treats it as nice-to-have).

## Not a requirement

- Login, user accounts, or profiles.
- Sharing history with Telegram.
- Booking or payments.
- Voice, image, or map input.
- Pixel-perfect or highly branded design.
- Hebrew or any language other than English.

## Acceptance

- Open the app, see the empty-state hint, send “Find me a hotel in Rome”, see a pending bubble and “Finding stays…”, then 5 scannable stays with a View listing link each.
- Send “I need a place to stay”, see a destination question that looks like a normal reply (not an error, not 5 fake hotels), then “Rome”, then 5 results. Composer focused for the follow-up.
- Send “cheaper ones” and see a new 5 still about Rome, with previous turns still visible and the new cards scrolled into view.
- If the LLM/search fails, see an error in the transcript with a next step, then send again. If the HTTP call fails, see status near the composer, the draft still in the box, then send again.
- Narrow viewport: composer remains usable; latest reply is not hidden behind it.
- The same trip on Telegram is a different conversation; the web UI does not show it.
