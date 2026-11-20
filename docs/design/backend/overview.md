# Backend service design

Java Spring process at `services/backend`. One process, four domains. Callers: web client and messaging service. This process never talks to WhatsApp.

Implement **recommendation → assistant → conversation → HTTP**.

## How a domain is built

Every domain uses the same kinds of parts. Spring `@Configuration` binds each interface to one implementation.

| Role | What it is | What it is not |
|---|---|---|
| **DTO (`Bl*`)** | Business types the managers speak | HTTP JSON, persistence rows |
| **DTO (`Api*`)** | HTTP JSON only | Used inside managers |
| **Interface** | Port another domain or infra must implement | A concrete engine |
| **Manager** | Orchestrates one domain’s rules; depends only on interfaces | SQL, HTTP, WhatsApp, an LLM SDK |
| **Repository** | Load/save domain data | Business rules |
| **Mapper** | `Db*` ↔ `Bl*`, or `Api*` ↔ `Bl*` | Rules |
| **In-memory stub** | Repository (or provider) that holds maps in process | Production DB (not required) |
| **Infra provider** | Talks to the outside world (LLM HTTP) behind an interface | Domain rules |
| **Command executor** | One use-case: `Bl` in, result out; calls managers | Parsing HTTP |
| **Request mapper / schema / response handler / error handler** | HTTP pipeline only | Domain |
| **Route handler** | Runs: validate → map to Bl → execute → map to Api | Business logic |
| **Factory** | Wires the pipeline for one feature | Logic |

Not in this service (not in requirements): login, JWT, rate limits, audit logging, Redis, SQL.

**Errors:** domain throws / returns **BL errors**. HTTP error handler maps those to JSON. Inner domains do not set HTTP status codes.

## Package layout

```
com.vacationary.backend
  BackendApplication
  bootstrap/          Spring config: bind interfaces → implementations
  constants/          stay types, outcome kinds, HTTP statuses
  core/interfaces/    every port (managers, repositories, llm, http pipeline)
  dtos/               Bl* (all domains) and Api* (HTTP only)
  errors/bl/          business errors
  errors/http/        API error body
  domain/
    recommendation/
    assistant/
    conversation/
  persistence/
    inmemory/         stub stores + Db mappers
  infra/
    llm/              ILlmClient HTTP adapter
    recommendation/   any-city provider
  http/
    spring/           generic RouteHandler, response writer, error handler
    features/turns/
    features/history/
    factories/
    routes/
```

## Domain graph

```
http.features.*  --CommandExecutor-->  ConversationManager
                                          │
                                          ├── ConversationRepository
                                          ├── ProcessedEventRepository
                                          └── IAssistantManager (AssistantManager)
                                                 │
                                                 ├── ILlmManager (LlmManager)
                                                 │      ├── ILlmPromptBuilder
                                                 │      └── ILlmClient
                                                 └── IStayManager (StayManager)
                                                        │
                                                        ├── IStayRepository
                                                        └── IStayProvider
```

Follow-ups work because the **message list** for that conversation is passed into the assistant. There is no separate stored destination or filter snapshot.

Inner docs:

- [recommendation.md](recommendation.md) — repository, provider, manager flow
- [assistant.md](assistant.md) — prompt, LLM, parser, response mapping
- [conversation.md](conversation.md) — stores, manager turn loop
- [http-contract.md](http-contract.md) — pipeline pieces and JSON
