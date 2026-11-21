# Telegram adapter — inner design

Only v1 platform. Bot API long poll in, `sendMessage` out. Implement last.

Shared turn rules (clarify / recommend / fail) are in [system-behavior.md](../../requirements/system-behavior.md). Backend `POST /turns` is in [http-contract.md](../backend/http-contract.md). This file is the Telegram **edge**: Bot API → this process → backend → Bot API.

## End-to-end flow

v1 does **not** expose a webhook. A poller inside this process calls Telegram.

### 1. Process starts

```
MessagingApplication
  → TelegramUpdatePoller (ApplicationRunner)
       if bot token blank → log "Telegram is not configured"; do not poll (tests)
       else loop until shutdown:
            TelegramUpdateHandler.poll
                 GET {api}/bot{token}/getUpdates
                      ?timeout=30
                      &offset={lastUpdateId + 1}
                      &allowed_updates=["message"]
                 consume result[]
            on HTTP/JSON error → wait a few seconds, retry; do not crash the process
```

`offset` is in-memory only. After a restart Telegram may resend recent updates; backend `eventId` = `update_id` skips a second assistant run and a second send (T-09, M-10).

### 2. User sends a 1:1 text to the bot

```
User Telegram text (private chat)
  → Telegram Bot API
  → getUpdates result[] item
  → IInboundParser.parse (one Update JsonNode)
       keep when:
            message exists
            message.chat.type = private
            message.text is non-blank
            message.from.id present
            update_id present
       skip groups, channels, media, stickers, edited_message, empty text
       kept row → BlInboundMessage(
            channel        = "telegram",
            externalUserId = message.from.id as decimal string,
            text           = message.text  (includes /start as text),
            eventId        = update_id as decimal string)
  → IInboundReplyManager.process   [inbound.md]
       IBackendClient.postTurn → POST http://localhost:8080/turns
            { channel, externalUserId, text, eventId }
       backend RouteHandler (validate → map → execute → respond)
            duplicate eventId → 200 with duplicate=true, assistant not run again
            else load that telegram+user-id conversation, assistant, save turn, save event
       if duplicate=true → empty (poller does not sendMessage)
       else BlOutboundMessage(user id, IReplyRenderer.render(result))
            recommend → intro (markdown tables stripped) + 5 stays
                         (name, Hotel/…, location, price, rating, why, URL on its own line)
            clarify / refuse_scope / fail → assistant text only
            backend unreachable → "The assistant could not complete that turn. Please retry."
  → if a reply exists: IMessageSender.send(to, body)
       TelegramBotClient
       POST {api}/bot{token}/sendMessage
       { chat_id: to, text: body }
       no parse_mode (plain text; Telegram still makes http URLs tappable)
       send throw → one short "Please retry" send; if that fails too, swallow.
            Offset still advances; do not loop retries of the original (M-11)
  → after the batch: lastUpdateId = max update_id seen (including skipped non-text)
```

Conversation key on the backend is `telegram` + Telegram user id. A web session is a different conversation. Private `chat.id` equals `from.id`; send with that id.

### 3. What the user sees (same outcomes as web)

| Backend `outcome` | Telegram message |
|---|---|
| `recommend` | Short intro + five stays, OTA URL tappable |
| `clarify` | One destination question |
| `refuse_scope` | Lodging-only refusal |
| `fail` | What failed and retry |
| HTTP to backend failed | Retry sentence above |
| `duplicate=true` | Nothing new (first reply already sent, or send already failed — M-11) |
| `sendMessage` failed | One short “Please retry”; silence only if that fails too |

Non-text or group updates: parse empty → advance offset, no backend call.

## Inner parts

```
TelegramUpdatePoller
  └── retry loop
        TelegramUpdateHandler.poll
          TelegramBotClient.getUpdates
          consume / deliver
            IInboundParser          → TelegramInboundParser
            IInboundReplyManager    (shared)
            TelegramBotClient.send  (IMessageSender)
```

| Part | Responsibility |
|---|---|
| `TelegramUpdatePoller` | Long poll lifecycle. Daemon thread, retry `handler.poll()`, stop on shutdown. Does not talk to Telegram. |
| `TelegramUpdateHandler` | `getUpdates`, track `offset`, parse → process → send per private text. Catch send errors. |
| `TelegramInboundParser` | One `Update` JSON → 0 or 1 `BlInboundMessage`. Private text only. |
| `TelegramBotClient` | `getUpdates` / `sendMessage` with bot token. Blank token → send throws (handler swallows). |

No webhook controller in v1. Inbound is the poller.

## HTTP

This process still binds **8081** (Boot default for the service). v1 inbound is **not** an HTTP route. Optional later: `POST /webhooks/telegram` if webhook is added (T-13 allows it; do not build it in v1).

`channel` on backend calls is always `telegram`. `externalUserId` is `message.from.id`. `eventId` is `update_id`.

Backend `HttpChannels` / `ConversationChannel` must accept `telegram`. That is a small backend change with this adapter, not an assistant change.

## Config

`VACATIONARY_BACKEND_BASE_URL` (default `http://localhost:8080`), `VACATIONARY_TELEGRAM_BOT_TOKEN`, optional `VACATIONARY_TELEGRAM_API_BASE_URL` (default `https://api.telegram.org`).

Blank bot token: poller does not call Telegram (tests use a fake sender / no loop).

## Why 1:1 Telegram

Travel help is a private chat with one traveler. Telegram maps one user id to one conversation key (`telegram` + id). BotFather is enough to go live.

## Out of this adapter

- Assistant, catalog, conversation store
- Web client
- Inline keyboards, groups, channels, media search, webhook server
