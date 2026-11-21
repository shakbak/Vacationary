# Web client

React + TypeScript chat at `services/web`. Talks to the backend **directly** (`POST /turns`, `GET /conversations/web/{id}`). No messaging service, no LLM, no stay search.

Design: [docs/design/web](../../docs/design/web/overview.md)

Requirements: [docs/requirements/web.md](../../docs/requirements/web.md)

## Run

Needs **Node 20+** (Vite will not run on older Node). From this directory:

```bash
npm install
npm test
npm run dev
```

The Vite dev server (port 5173) proxies `/turns` and `/conversations` to the backend on `localhost:8080`. Start the backend first. Set `VACATIONARY_LLM_API_KEY` on the backend for a real assistant; tests here do not call the network.

Optional `VITE_API_BASE_URL` (see `.env.example`) is for a hosted origin. Do not add Spring CORS until that origin exists.

## Session and storage

Anonymous user id lives in `sessionStorage` (`vacationary.web.userId`). Refresh in this tab reuses it. A new tab is a new user. Conversations are **in-process H2** on the backend: they are lost on backend restart, are not shared across instances, and are not durable. If history cannot be loaded, the UI starts empty and still lets you chat.
