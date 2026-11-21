# Inbound turn — inner design

Platform-agnostic: one user text in, at most one rendered reply out. WhatsApp types stay in the adapter.

Implement after **render** (pure string) and **backend client**.

## Inner parts

```
ITurnProcessor
  ├── IBackendClient     POST /turns
  └── IReplyRenderer     BlTurnResult → WhatsApp English
        └── StayTypeLabel, table strip
```

| Part | Responsibility |
|---|---|
| `TurnProcessor` | Call backend; skip send on `duplicate`; otherwise render and `IMessageSender.send`. On backend transport failure, still send fail copy. |
| `HttpBackendClient` | JSON `channel=whatsapp`, `externalUserId`, `text`, `eventId`. Map 200 body to `BlTurnResult`. Network/4xx/5xx → fail result, not an exception the webhook turns into 500. |
| `ReplyRenderer` | `recommend`: intro (tables stripped) plus five stays (name, type words, location, price, rating, why, URL). Other outcomes: assistant `text` only. |

## Types

**`BlInboundMessage`:** `channel`, `externalUserId`, `text`, `eventId`.

**`BlTurnResult`:** `outcome`, `text`, `recommendations[]`, `duplicate`.

**`BlStayOption`:** same fields as backend `ApiStayOption` (this service copies JSON; it does not import backend packages).

## How a turn runs

```
TurnProcessor.process(inbound, sender)
  result = IBackendClient.postTurn(inbound)
  if result.duplicate → return (no WhatsApp message)
  body = IReplyRenderer.render(result)
  IMessageSender.send(inbound.externalUserId, body)
```

Backend already ignores duplicate `eventId` (B-15). This skip is what stops a **second user-visible** WhatsApp message.

Fail copy when the backend cannot be reached: `The assistant could not complete that turn. Please retry.`

## Tests

- Recommend renders five stays with type words and a URL line.
- Clarify/fail is plain text, no stay list.
- `duplicate=true` does not call the sender.
- Backend transport failure still sends the retry sentence.
