# WhatsApp adapter — inner design

Only v1 platform. Cloud API webhook in, Graph send out. Implement last.

## Inner parts

```
WhatsAppWebhookController          thin GET/POST /webhooks/whatsapp
  └── IWhatsAppWebhookHandler
        GET  → IWebhookVerifier
        POST → IInboundParser → ITurnProcessor (per text message)
IMessageSender ← WhatsAppCloudClient   Graph POST /{phone-number-id}/messages
```

| Part | Responsibility |
|---|---|
| `WhatsAppWebhookVerifier` | `hub.mode=subscribe` and `hub.verify_token` match config → return `hub.challenge`. Else 403. |
| `WhatsAppInboundParser` | Cloud API JSON → text messages only (`from`, `id`, `text.body`). Status callbacks and media → empty list. |
| `WhatsAppCloudClient` | Send `{ messaging_product, to, type: text, text: { body, preview_url: true } }` with the access token. |
| `WhatsAppWebhookHandler` | Always finish POST with **200** so Meta does not hammer retries for handler bugs; processing errors become a fail send when possible. |

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
