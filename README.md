# Vacationary

Lodging-only travel assistant for the Channels Challenge. Users talk to the **same** assistant on **web** and **Telegram**. English only. Destination is required; dates, budget, guest count, stay type, and amenities are optional. A typical recommend reply is **five** stays (name, type, area, price, rating, why, OTA link).

Repo: [github.com/shakbak/Vacationary](https://github.com/shakbak/Vacationary)

## Assignment deliverables

| PDF item | Where it is |
|---|---|
| GitHub repo, setup, architecture, design, limitations | This README + [docs/design](docs/design/README.md) + [docs/requirements](docs/requirements/README.md) |
| `.env.example` without credentials | [`.env.example`](.env.example), [services/web/.env.example](services/web/.env.example), [services/messaging/.env.example](services/messaging/.env.example) |
| Interactive demo (working link + access) | Telegram: [t.me/VacationaryBot](https://t.me/VacationaryBot) — see [Talk to it](#talk-to-it-reviewers) |
| Demo video (web + Telegram, follow-up, failure) | Record using [Demo video](#demo-video) (not in the repo yet) |
| What we would improve with more time | [With more time](#with-more-time) |

## Talk to it (reviewers)

**Telegram (preferred live path).** Open [t.me/VacationaryBot](https://t.me/VacationaryBot) or search `@VacationaryBot`. Message in **English**. A city is enough (`Hotels in Barcelona`). Dates, budget, and party size are optional.

The bot is a **locally running** service (assignment allows that). Backend + messaging must be up on the machine that holds `VACATIONARY_TELEGRAM_BOT_TOKEN`. There is no public HTTPS webhook.

**Web.** After [Run](#run): open `http://localhost:5173`. Same assistant, a **different** conversation (not linked to Telegram).

## Run

Needs **JDK 21+** and **Node 20+**. Groq key: gitignored `sec.txt` at the repo root (first non-comment line) or `VACATIONARY_LLM_API_KEY`. Never commit keys or the bot token.

**1. Backend** (port 8080) — conversations, LLM, stay search:

```bash
cd services/backend
# Windows: mvnw.cmd
./mvnw spring-boot:run
```

**2. Web** (port 5173, proxies API to 8080):

```bash
cd services/web
npm install
npm run dev
```

**3. Telegram** (port 8081, long-polls Bot API, calls 8080):

```bash
cd services/messaging
export VACATIONARY_TELEGRAM_BOT_TOKEN=your-bot-token
# Windows: $env:VACATIONARY_TELEGRAM_BOT_TOKEN = "your-bot-token"
./mvnw spring-boot:run
```

Tests (no live Groq or Telegram): `./mvnw test` in `services/backend` and `services/messaging`; `npm test` in `services/web`.

## Architecture

Three processes, one assistant:

```
Web (Vite/React)  ──POST /turns──►  Backend (Spring, :8080)
                                    conversations + Groq + stay catalog (H2)
Telegram user     ──Bot API──►  Messaging (Spring, :8081)  ──POST /turns──►  Backend
```

- **Backend** owns history, isolation (`channel` + user id), idempotency (`eventId` / Telegram `update_id`), the LLM, and search. It does not talk to Telegram.
- **Web** calls the backend directly. Anonymous id in `sessionStorage`.
- **Messaging** long-polls Telegram, renders the backend outcome as English text, sends `sendMessage`. No LLM, no catalog.

Channel-specific code stays in the web client or the Telegram adapter. Assistant and recommendation do not change per channel.

## Design decisions

- **Lodging only.** Flights/tours/restaurants are refused. Sample OTA URLs are allowed.
- **Hybrid chat.** One complete message can search. Missing city → one clarification question, not a form.
- **Telegram 1:1 DMs**, not groups. Travel help is a private chat; BotFather is enough to go live; long polling avoids a public webhook and Telegram’s ~60s webhook timeout while Groq runs.
- **Five results, English only**, on both channels.
- **In-process H2** for conversations and the catalog (allowed). Documented below.
- **No account linking** between web and Telegram.
- **Assignment extras** (buttons, cancel, handoff, proactive notify) were left unfinished on purpose: a working core over half-built extras.

Inner design: [docs/design](docs/design/README.md). Requirements: [docs/requirements](docs/requirements/README.md).

## Known limitations

- Restarting the backend **wipes** conversations and processed events. They are not shared across instances and are not durable.
- Restarting **backend and messaging together** can replay recent Telegram updates and produce a second reply (both stores empty). Messaging-only restart is fine (backend still has the event).
- If Telegram cannot deliver even a short retry line, the user sees nothing new; the same `update_id` is not sent again.
- Catalog and OTA links are **sample data**, not live booking.
- Telegram has no buttons, groups, or media search. Web send is disabled while a turn is in flight (no cancel).
- The live bot only answers while this machine is running both Java processes.

## Demo video

Record a short clip that shows all three assignment beats:

1. **Web** — `http://localhost:5173`, send `Find me a hotel in Rome`, wait for five cards.
2. **Telegram** — [t.me/VacationaryBot](https://t.me/VacationaryBot), same kind of request.
3. **Follow-up** — on either channel, `cheaper` or `near the beach` (still Rome).
4. **Failure** — stop the backend or remove the LLM key, send again, show the error/retry copy, then recover.

Put the file or hosting link in this README when you have it.

## With more time

- Persist conversations (and Telegram poll offset) so restarts do not wipe trips or replay updates.
- Host the web UI and keep the bot process up for the whole review window without a laptop.
- Telegram quick-reply buttons for follow-ups (`Cheaper`, `Start over`) with the same text fallback.
- Optional link between a web session and a Telegram user, with an explicit code, not silent merge.
- Cancel an in-flight Groq call when the user sends a correction.

## Layout

| Path | Role |
|---|---|
| `services/backend` | Assistant, recommendation, HTTP `POST /turns` |
| `services/web` | React chat |
| `services/messaging` | Telegram adapter |
| `docs/requirements` | What must be true |
| `docs/design` | How each service is built |
