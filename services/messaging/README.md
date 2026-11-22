# Messaging platforms service

Separate Java process for **Telegram only**. It long-polls the Bot API, calls the backend `POST /turns`, and sends the same assistant outcome back as English Telegram text. It does not run the LLM or search stays.

Design: [docs/design/messaging](../../docs/design/messaging/overview.md)

Requirements: [docs/requirements/messaging.md](../../docs/requirements/messaging.md)

## Why 1:1 Telegram

Travel help is a private chat with one traveler. Telegram maps one user id to one conversation key (`telegram` + id). BotFather is enough to go live. Groups and channels are out of scope.

## Run

Needs JDK 21+. Start the **backend** on port 8080 first (`VACATIONARY_LLM_API_KEY` or repo-root `sec.txt`). Then from this directory:

```bash
# Windows: mvnw.cmd
export VACATIONARY_TELEGRAM_BOT_TOKEN=your-bot-token
./mvnw spring-boot:run
```

Listens on **8081** (process health). Inbound is **long polling**, not a webhook. Tests: `./mvnw test` (no Telegram or backend network).

## Talk to the bot (reviewers)

Open [t.me/VacationaryBot](https://t.me/VacationaryBot) or search `@VacationaryBot` in Telegram. Message in **English**. A city is enough (`Hotels in Barcelona`). Dates, budget, and party size are optional.

This process must be running on the review machine (with the backend on 8080). To recreate the bot: [@BotFather](https://t.me/BotFather) `/newbot`, set `VACATIONARY_TELEGRAM_BOT_TOKEN`, start this service.

You should get one Telegram reply from the shared assistant: a clarification question if no city, or five stays with type, area, price, rating, why, and a tappable OTA link. English only.

If the backend restarts, in-memory conversations are wiped (same limit as web). If Telegram rejects the full reply, the bot sends a short “please retry” once. If that also fails, a later poll of the same `update_id` will not send another copy (backend duplicate id).

## Limits

- No other messaging-platform adapters in v1.
- No buttons, groups, or media-as-search.
- Web chat does not go through this service.
- A public HTTPS URL is not required for v1.
