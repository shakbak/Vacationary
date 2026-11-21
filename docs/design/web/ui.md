# UI — inner design

One English chat screen. Usability over visual complexity (WEB-02, WEB-23–WEB-34). Implement last; it only reads chat state and calls `loadHistory` / `sendTurn`.

No login. No extra intake form. No extra hotels beyond the five from the backend (WEB-18, WEB-19).

## Inner parts

```
App
  ├── Transcript     empty state, or messages + pending user bubble + loading row
  │     ├── Message    user or assistant text (`fail` uses error treatment)
  │     └── StayCard   one option (recommend only)
  ├── Status         HTTP/network error only (turn not saved); optional history-load note
  └── Composer       labeled field + send; disabled while loading; keeps draft on HTTP error
```

| Part | Responsibility |
|---|---|
| `App` | Mount → `loadHistory`. Column: header, scrolling transcript, status, pinned composer. |
| `Transcript` | Oldest at top. Empty state when there are no messages. Scroll latest into view (WEB-26). |
| `StayCard` | Scannable card: name first, human stay type, location, price, rating, why, **View listing**. |
| `Status` | HTTP/network error and optional “could not load earlier messages”. Not used for clarify. |
| `Composer` | Accessible name, Enter to send, `aria-busy` while in flight. Focus after each attempt (WEB-25). |

## Layout

Full-height column (~100dvh). Header (Vacationary, one-line “lodging recommendations”) does not scroll away. Transcript `flex: 1; overflow-y: auto`. Composer (and HTTP status) stay at the bottom.

Works at a typical laptop width and at ~360px (WEB-27). Content `max-width` ~40rem, centered, padding so cards are not edge-to-edge on mobile. The composer must not cover the latest bubble, loading row, or stay cards — transcript padding-bottom matches composer height, or the scroll target is the loading/reply node.

Stay options are stacked cards, not a carousel. No photos.

## Empty state (WEB-23)

When there are zero committed messages and nothing is in flight, the transcript shows short English copy, for example:

- “I recommend places to stay.”
- “Tell me a city to get started. Dates, budget, and number of people are optional.”

Not a form, not an error, not five placeholder hotels. It disappears after the first send.

## States

| Situation | User sees | Send | Composer text |
|---|---|---|---|
| First load | Brief “Loading…” only if the GET is slow; then messages or empty state | Enabled when load settles | Empty |
| History GET failed | Empty state + optional Status note (WEB-34) | Enabled | Empty |
| Turn in flight | Pending user bubble + in-transcript “Finding stays…” (WEB-28). `aria-busy` | Disabled | Cleared or ignored; pending shows the text |
| `recommend` | Assistant text + **5** stay cards | Enabled | Empty; focus in field |
| `clarify` / `refuse_scope` | Ordinary assistant bubble (WEB-15, WEB-29) | Enabled | Empty; focus |
| `fail` (200) | Assistant **error** bubble in the transcript (backend `text`). No cards | Enabled | Empty; focus |
| HTTP / network error | Pending bubble gone. Status under transcript: could not reach assistant, retry. Draft restored | Enabled | **Same text** (WEB-24) |

**Clarify is success.** **Fail** is a saved assistant error in the thread. **HTTP error** is status next to the composer because the turn never landed. Distinguish with wording (and optional icon), not color alone (WEB-30). Errors stay until the next send starts (WEB-33).

Loading copy: “Finding stays…”. HTTP fallback: “Could not reach the assistant. Please retry.” Prefer backend `text` / `{ message }` when present.

## Stay cards (WEB-31)

Render **only** on recommend. All five, in order, each with a clear hierarchy:

1. **Name** (heading)
2. Stay type as ordinary words: Hotel, Motel, Vacation rental, Hostel, B&B, Resort — never `VACATION_RENTAL` / `bnb`
3. Location / area
4. Price (may include nights when the backend sent dates) and rating on one secondary line
5. Why (short paragraph)
6. **View listing** → `url`, `target="_blank"`, `rel="noreferrer noopener"`

Do not invent a sixth card. Do not hide fields the backend sent. Do not make the whole card a single click target that hides the why.

## Composer (WEB-32)

- Visible label or `aria-label` e.g. “Message”. Placeholder can repeat the empty-state hint (“A city is enough…”).
- Single-line field + Send is enough. Enter sends; blank / whitespace does not.
- While in flight: field and button disabled; loading copy in the transcript explains the wait.
- No “new conversation” control. The user can type “start over”.

After every send attempt completes, `focus()` the field (WEB-25).

## Motion and contrast

Scroll into view with `scrollIntoView` on the pending bubble, loading row, or new assistant block. Prefer a small `block: "end"` so the composer stays on screen.

Keep text contrast readable on a light background; do not rely on red vs gray alone to mean error vs clarify.

## Tests (UI)

Focused, with a fake `chat` layer:

- Empty state visible when there are no messages.
- Send disabled during loading; in-transcript loading copy present.
- Recommend shows five cards, human stay type, View listing links.
- Clarify is an ordinary assistant message, not Status.
- `fail` is an error in the transcript; HTTP error is Status and the draft remains.
- Composer is focused after a finished send (success or error).

Session reuse is tested in the session/chat layer, not here.

## Out of this layer

- `fetch`, sessionStorage, event ids
- Telegram, login, maps, voice, Hebrew
- Progress/cancel, photos, account linking
