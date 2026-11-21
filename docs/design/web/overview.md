# Web client design

React + TypeScript Vite SPA at `services/web`. One screen: chat with the same assistant as Telegram. This client **renders** conversations; it does not search stays, call the LLM, or talk to the messaging platforms service.

Call the backend **directly**: `POST /turns` and `GET /conversations/{channel}/{externalUserId}`. Always `channel = web`. JSON contract: [backend/http-contract.md](../backend/http-contract.md). Product rules: [docs/requirements/web.md](../../requirements/web.md).

Implement **api → session → chat → ui**.

## How the client is built

Small layers. No Redux, no Next.js, no login. TypeScript uses ordinary types — **no `I` prefix** (that was a Java convention).

| Layer | What it is | What it is not |
|---|---|---|
| **session** | Anonymous web user id in `sessionStorage`; new `eventId` per send | Telegram id, account linking, cookies |
| **api** | Typed `fetch` for turns and history; HTTP vs `fail` outcome | Assistant logic, stay search |
| **chat** | Load history, send a turn, lock send while in flight | DOM, CSS |
| **ui** | Transcript, stay cards, composer, loading/error copy | Backend calls of its own |

Not in this client (not in requirements): photos, cancel, a “new conversation” button, typing while a reply is loading, SSR, routing beyond the one screen.

## Folder layout (when implemented)

```
services/web
  src/
    session/     get or create web user id; new event id
    api/         JSON types + fetch client
    chat/        loadHistory, sendTurn, in-flight lock
    ui/          App, Header, Status
      replyText/   stripReply, whyForStay, markdownTable, numberedStay
      transcript/  Transcript, Message, EmptyState
      stay/        StayList, StayCard, stayType
      composer/    Composer, useComposer
    main.tsx
```

Vite dev server proxies `/turns` and `/conversations` to the backend on `localhost:8080` so the browser stays same-origin. Do **not** add Spring CORS until a hosted web origin needs it. Document that for the demo.

In-memory backend limits (B-12) apply here: refresh restores history only if this tab still has the session id **and** the backend process still has that conversation. Otherwise the UI starts empty.

## Layer graph

```
ui
  └── chat            loadHistory / sendTurn
        ├── session   web user id, eventId
        └── api       GET history, POST turn
              └── backend HTTP (port 8080)
```

Inner docs:

- [chat.md](chat.md) — session, API client, send/load/lock, outcome mapping
- [ui.md](ui.md) — transcript, cards, composer, loading/success/error, usability (empty state, layout, a11y)
