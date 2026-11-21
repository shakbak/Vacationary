# WhatsApp adapter — inner design

Only v1 platform. Cloud API webhook in, Graph send out. Implement last.

Shared turn rules (clarify / recommend / fail) are in [system-behavior.md](../../requirements/system-behavior.md). Backend `POST /turns` is in [http-contract.md](../backend/http-contract.md). This file is the WhatsApp **edge**: Meta → this process → backend → Graph.

## End-to-end flow

Two HTTP shapes hit the same controller (`WhatsAppWebhookController` on port 8081).

### 1. Meta verifies the webhook (once, when you save the callback URL)

```
Meta GET /webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=…&hub.challenge=…
  → WhatsAppWebhookController.verify
  → IWhatsAppWebhookHandler.verify
  → IWebhookVerifier.matches(mode, token)
       token equals VACATIONARY_WHATSAPP_VERIFY_TOKEN and mode is subscribe
       → HTTP 200, body = hub.challenge (plain text)
       else → HTTP 403, empty body
```

No backend call. No WhatsApp send.

### 2. User sends a 1:1 text to the business number

```
User WhatsApp text
  → Meta Cloud API
  → POST /webhooks/whatsapp   (raw JSON body)
  → WhatsAppWebhookController.inbound
       always HTTP 200 when the method returns
       (Meta retries on non-2xx; we must not fail the webhook because the LLM is slow or Graph send failed)
  → IWhatsAppWebhookHandler.inbound
  → IInboundParser.parse
       walk entry[].changes[].value.messages[]
       keep type=text with from, id, text.body
       skip statuses, images, empty body
       each kept row → BlInboundMessage(
            channel        = "whatsapp",
            externalUserId = from (wa_id / phone),
            text           = text.body,
            eventId        = messages[].id  (wamid.…))
  → for each BlInboundMessage: IInboundReplyManager.process   [inbound.md]
       IBackendClient.postTurn → POST http://localhost:8080/turns
            { channel, externalUserId, text, eventId }
            TurnResponseParser maps the 200 JSON body (or fail copy)
       backend RouteHandler (validate → map → execute → respond)
            duplicate eventId → 200 with duplicate=true, same stored outcome, assistant not run again
            else load that whatsapp+wa_id conversation, assistant, save turn, save event
       if duplicate=true → empty (handler does not Graph send)
       else BlOutboundMessage(wa_id, IReplyRenderer.render(result))
            recommend → intro (markdown tables stripped) + 5 stays
                         (bold name, Hotel/…, location, price, rating, why, URL on its own line, preview_url)
            clarify / refuse_scope / fail → assistant text only
            backend unreachable → "The assistant could not complete that turn. Please retry."
  → if a reply exists: IMessageSender.send(to, body)
       WhatsAppCloudClient
       POST {graph}/v21.0/{phone-number-id}/messages
       Bearer access token
       { messaging_product, to: wa_id, type: text, text: { body, preview_url: true } }
       Graph send throw → swallow; webhook already 200; do not loop retries
```

Conversation key on the backend is `whatsapp` + `wa_id`. A web session is a different conversation.

### 3. What the user sees (same outcomes as web)

| Backend `outcome` | WhatsApp message |
|---|---|
| `recommend` | Short intro + five stays, OTA URL tappable |
| `clarify` | One destination question |
| `refuse_scope` | Lodging-only refusal |
| `fail` | What failed and retry |
| HTTP to backend failed | Retry sentence above |
| `duplicate=true` | Nothing new (first reply already sent, or send already failed — M-11) |

Webhook POST with only delivery `statuses` (no `messages`) parses to an empty list → 200, no backend call.

## Inner parts

```
WhatsAppWebhookController          thin GET/POST /webhooks/whatsapp
  └── IWhatsAppWebhookHandler
        GET  → IWebhookVerifier
        POST → IInboundParser → IInboundReplyManager → IMessageSender (if a reply)
IMessageSender ← WhatsAppCloudClient   Graph POST /{phone-number-id}/messages
```

| Part | Responsibility |
|---|---|
| `WhatsAppWebhookVerifier` | `hub.mode=subscribe` and `hub.verify_token` match config → return `hub.challenge`. Else 403. |
| `WhatsAppInboundParser` | Cloud API JSON → text messages only (`from`, `id`, `text.body`). Status callbacks and media → empty list. |
| `WhatsAppCloudClient` | Send `{ messaging_product, to, type: text, text: { body, preview_url: true } }` with the access token. |
| `WhatsAppWebhookHandler` | Always finish POST with **200** so Meta does not hammer retries for handler bugs. Parse → process → send. Graph send errors are swallowed (M-11). |

No Telegram/Slack classes.

## HTTP

Port **8081**.

- `GET /webhooks/whatsapp?hub.mode&hub.verify_token&hub.challenge`
- `POST /webhooks/whatsapp` raw Cloud API body

`channel` on backend calls is always `whatsapp`. `externalUserId` is the sender `wa_id`. `eventId` is WhatsApp `messages[].id` (`wamid.…`).

## Config

`VACATIONARY_BACKEND_BASE_URL` (default `http://localhost:8080`), `VACATIONARY_WHATSAPP_VERIFY_TOKEN`, `VACATIONARY_WHATSAPP_ACCESS_TOKEN`, `VACATIONARY_WHATSAPP_PHONE_NUMBER_ID`, optional Graph base URL.

Blank send token: client reports send failure (tests use a fake sender).

## Out of this adapter

- Assistant, catalog, conversation store
- Web client
- Buttons, lists, templates, groups, media search
