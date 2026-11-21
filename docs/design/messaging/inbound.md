# Inbound turn — inner design

Platform-agnostic: one user text in, at most one rendered reply out. Telegram types stay in the adapter. Backend HTTP stays in infra.

Reuse the existing manager, renderer, and backend client. Only `channel` on `POST /turns` changes (`telegram`). Implement Telegram adapter last.

## Inner parts

```
IInboundReplyManager           inbound text → Optional<BlOutboundMessage>
  ├── IBackendClient           POST /turns → BlTurnResult
  │     └── TurnResponseParser JSON body → BlTurnResult
  └── IReplyRenderer           BlTurnResult → English body
```

| Part | Responsibility |
|---|---|
| `InboundReplyManager` | Domain inbound reply. Guard empty input; ask the backend; skip a duplicate; otherwise render. Returns a reply or empty. Does not parse Telegram JSON or call `sendMessage`. |
| `HttpBackendClient` | POST JSON `channel`, `externalUserId`, `text`, `eventId`. Channel comes from the inbound DTO (`telegram` in v1). Network/4xx/5xx → fail result, not an exception that kills the poller. |
| `TurnResponseParser` | Map a 200 `/turns` body to `BlTurnResult` (outcome, text, stays, duplicate). Missing/invalid JSON → fail result. |
| `ReplyRenderer` | `recommend`: intro (tables stripped) plus five stays (name, type words, location, price, rating, why, URL). Other outcomes: assistant `text` only. Same English body as before; Telegram autolinks `http` URLs. |

Delivery (`IMessageSender`) is the Telegram adapter, not this manager. The adapter sends only when `process` returns a reply.

## Types

**`BlInboundMessage`:** `channel`, `externalUserId`, `text`, `eventId`.

**`BlTurnResult`:** `outcome`, `text`, `recommendations[]`, `duplicate`.

**`BlStayOption`:** same fields as backend `ApiStayOption` (this service copies JSON; it does not import backend packages).

**`BlOutboundMessage`:** `to` (Telegram user / chat id as decimal string), `body` (rendered English).

Telegram is what *calls* this manager (poller → parse → `process` → maybe send). The Bot API path is in [telegram.md](telegram.md).

## How a turn runs

```
InboundReplyManager.process(inbound)
  if missing user or text → empty
  result = IBackendClient.postTurn(inbound)
  if result.duplicate → empty
  body = IReplyRenderer.render(result)
  return BlOutboundMessage(inbound.externalUserId, body)
```

Backend already ignores duplicate `eventId` (B-15). Returning empty here is what stops a **second user-visible** Telegram message.

Fail copy when the backend cannot be reached: `The assistant could not complete that turn. Please retry.` That is still a reply (the adapter sends it).

## Tests

- Recommend renders five stays with type words and a URL line.
- Clarify/fail is plain text, no stay list.
- `duplicate=true` returns empty (adapter does not send).
- Backend transport failure still produces the retry sentence.
- Parser maps `/turns` JSON; blank or missing `outcome` is fail.
