# Messaging platforms service — design

Separate Java Spring process at `services/messaging` (port **8081**). WhatsApp in, backend HTTP out. This process never runs the LLM, never stores canonical history, and never searches stays.

v1 implements **exactly one** platform: WhatsApp Cloud API. A later platform is a new adapter here plus a new `channel` on backend calls — not a change to assistant or recommendation.

Build order: **render → backend client → inbound processor → WhatsApp adapter → webhook HTTP**.

## How the service is built

Same roles as the backend: `Bl*` DTOs, `I*` ports, managers/processors that depend only on interfaces, infra adapters, thin HTTP.

| Role | What it is | What it is not |
|---|---|---|
| **`BlInboundMessage`** | channel, external user id, text, event id | WhatsApp JSON |
| **`BlTurnResult`** | backend outcome (`recommend` / `clarify` / `refuse_scope` / `fail`), text, stays, duplicate | Graph API payload |
| **`ITurnProcessor`** | One inbound message → maybe one outbound text | Parsing WhatsApp, calling Graph |
| **`IBackendClient`** | `POST /turns` | Assistant logic |
| **`IReplyRenderer`** | Outcome → English WhatsApp body (5 stays when recommend) | HTTP |
| **`IMessageSender`** | Deliver one text to a platform user | Backend |
| **WhatsApp adapter** | Verify webhook, parse Cloud API JSON, send via Graph | Shared core |

## Folder layout

```
services/messaging
  src/main/java/com/vacationary/messaging/
    MessagingApplication
    bootstrap/            bind interfaces → implementations
    core/interfaces/
    dtos/
    domain/inbound/       TurnProcessor
    domain/render/        ReplyRenderer, stay-type labels, strip tables
    infra/backend/        HttpBackendClient
    infra/whatsapp/       parser, verifier, Graph sender, webhook handler
    http/                 WhatsAppWebhookController
```

## Layer graph

```
GET/POST /webhooks/whatsapp
  └── IWhatsAppWebhookHandler
        ├── IWebhookVerifier          (GET hub.challenge)
        ├── IInboundParser            Cloud JSON → 0..n BlInboundMessage
        └── ITurnProcessor            (each text message)
              ├── IBackendClient      POST localhost:8080/turns  channel=whatsapp
              ├── IReplyRenderer      English body
              └── IMessageSender      Graph messages API
```

`duplicate=true` from the backend → **do not send** (M-10). Backend HTTP/network failure → still try to send a retry message on WhatsApp (M-09). If Graph send fails after the backend already succeeded, do not loop retries; a later webhook is skipped as duplicate (M-11).

Inner docs:

- [inbound.md](inbound.md) — processor, backend client, renderer
- [whatsapp.md](whatsapp.md) — Cloud API webhook and send
