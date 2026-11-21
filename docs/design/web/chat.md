# Chat layer — inner design

Session identity, typed HTTP, send/load, and mapping backend outcomes to chat state. **No DOM.** Implement after `api` types/client, before UI.

The UI calls **chat**, not `fetch`. Chat always sends `channel: "web"`. Follow-up context is the backend message list; this client does not keep a destination/filter snapshot.

## Inner parts

```
session
  getOrCreateUserId()     sessionStorage
  newEventId()            crypto.randomUUID per send

api
  getConversation(userId) GET /conversations/web/{userId}
  postTurn(body)          POST /turns

chat
  loadHistory()           session + api.getConversation
  sendTurn(text)          lock → pending bubble → postTurn → commit or drop
```

| Part | Responsibility |
|---|---|
| `session` | Stable anonymous `externalUserId` for this tab. Fresh `eventId` every send. |
| `api` | Typed JSON in/out. Distinguishes **200 body** from **400/500/network**. |
| `chat` | Orchestrates load and send. Holds transcript + loading + error. Locks send while a turn is in flight. |

## Session

Key: `vacationary.web.userId` in **`sessionStorage`**.

- First visit in this tab: create a UUID, store it, use it.
- Refresh: reuse the same id (WEB-21).
- New tab: empty `sessionStorage` → new user (WEB-20). Not a Telegram id. No linking (WEB-05).

Each `POST /turns` uses a new `eventId` (`crypto.randomUUID`). The composer is disabled while loading, so double-submit is not the web path. A retry after HTTP failure is a **new** event id (the previous turn never reached the store).

## API JSON

Same contract as [backend/http-contract.md](../backend/http-contract.md). Local Vite proxy, relative URLs.

**`POST /turns`**

Request: `channel` (`"web"`), `externalUserId`, `text`, `eventId`.

Response 200: `outcome` (`recommend` \| `clarify` \| `refuse_scope` \| `fail`), `text`, `recommendations[]`, `duplicate`. Processed **`fail` is 200**, not 5xx.

**`GET /conversations/web/{externalUserId}`**

Response 200: `{ messages: [{ role, text, recommendations }] }`. Unknown key → empty list, not an error.

**400 / 500:** `{ code, message }`. Transport/validation failure. Distinct from `outcome: fail`.

## Chat rules

```
loadHistory
  userId = session.getOrCreateUserId()
  GET /conversations/web/{userId}
  if 200 (including empty list) → show those messages
  if the call fails (backend restarted / network) → start empty;
       optional non-blocking note: earlier messages could not be loaded (WEB-34)
  never block the composer on a missing history

sendTurn(text)
  if loading or text blank → ignore
  clear previous HTTP error (WEB-33: replaced by this attempt)
  lock composer
  show pending user bubble (not yet in committed history)
  POST /turns { channel: "web", externalUserId, text, eventId: newEventId() }
  unlock composer
  if 200:
    commit user line + assistant line into history
    composer draft is empty
    map outcome (below)
  else:
    drop the pending bubble (backend did not save)
    restore `text` as the composer draft (WEB-24)
    HTTP error state near the composer; user can send again
```

Do **not** commit the user line until 200. On HTTP error the backend never stored the turn; keeping it in the transcript would diverge from history. Restoring it **in the composer** is required.

### Outcome mapping (HTTP 200)

| `outcome` | Chat state | Stay cards |
|---|---|---|
| `recommend` | Success. Assistant `text` plus **all 5** options (name, stayType, location, price, rating, why, url). | Yes |
| `clarify` | Success. Question in the transcript. Wait for the next message. **Not** the error banner (WEB-15). | No |
| `refuse_scope` | Success. Refusal text in the transcript. | No |
| `fail` | Assistant **error in the transcript** (what happened + retry/rephrase). Backend **did** save this turn. Composer empty. | No (WEB-16, WEB-29) |

`duplicate: true` shows the stored outcome the same way; the web client does not generate a second assistant run.

### HTTP / network failure

Status near the composer, not a fake assistant message (WEB-29). Composer unlocked and **draft restored** (WEB-17, WEB-24). History unchanged. Copy says the request did not go through and to retry. Must not look like a hotel list. Stays until the next send starts (WEB-33).

## Types (client)

**`TurnRequest`:** `channel`, `externalUserId`, `text`, `eventId`.

**`TurnResponse`:** `outcome`, `text`, `recommendations`, `duplicate`.

**`StayOption`:** `name`, `stayType`, `location`, `price`, `rating`, `why`, `url`.

**`HistoryMessage`:** `role` (`user` \| `assistant`), `text`, `recommendations`.

**`ChatError`:** either `outcome: fail` (saved) or transport (`code` / network). UI copy differs; both unlock send.

## Wiring

```
chat ← session + api
ui   ← chat
```

Tests (this layer): session id reused from `sessionStorage`; send locked while in flight; 200 `fail` vs 500 vs `clarify`; HTTP error restores the sent text as draft.

## Out of this layer

- DOM, CSS, stay-card layout
- Stay search, LLM, Telegram
- Photos, cancel, extra intake fields
