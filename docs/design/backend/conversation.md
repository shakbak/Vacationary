# Conversation domain — inner design

Isolation, history, idempotency receipts. **Managers + repositories + Db mappers + H2 JDBC stores.**

Implement after assistant.

The conversation store keeps **messages**, not a destination or filter snapshot. Follow-up context is those messages. Processed events are a separate delivery receipt. Conversation does **not** call the assistant. **Turns HTTP** (`CommandExecutor`) orchestrates: getEvent → maybe return duplicate → else load history, call assistant, save turn, save event.

## Inner parts

```
IProcessedEventManager           getEvent/save (BlEventKey) → stored outcome
  ├── IProcessedEventRepository
  └── ProcessedEventMapper       DbProcessedEvent ↔ BlProcessedEvent

IConversationManager             getConversation(key); saveTurn(key, text, response)
  ├── IConversationRepository
  └── ConversationMapper         DbConversation ↔ BlConversation
```

| Part | Responsibility |
|---|---|
| `IProcessedEventManager` | `getEvent` / `save` a `BlProcessedEvent`. No chat, no assistant, no `duplicate` flag. |
| `IConversationManager` | `getConversation(key)`; `saveTurn(key, text, response)` persists user + assistant together. No `eventId`, no assistant call. |
| `IConversationRepository` | `find(key)`, `save(conversation)` |
| `IProcessedEventRepository` | `find(BlEventKey)`, `save(...)` |
| `JdbcConversationStore` | H2 tables: `conversations`, `messages`, `message_stay_options` |
| `JdbcProcessedEventStore` | H2 tables: `processed_events`, `processed_event_stay_options` |
| `stay_options` | Shared snapshot rows; link tables hold only parent + `seq` + `stay_option_id` |
| `DbConversation` / `DbMessage` | Persistence shape: key + messages |
| `DbStayOption` | Persistence snapshot of a shown stay (not catalog `DbStayListing`) |
| `ConversationMapper` | `DbConversation` ↔ `BlConversation` |
| `ProcessedEventMapper` | `DbProcessedEvent` ↔ `BlProcessedEvent` |
| `BlConversation`, `BlMessage`, `BlTurnCommand`, `BlTurnResult` | Manager / HTTP-turn language |
| `BlProcessedEvent` | Stored delivery: `BlEventKey` + assistant outcome |
| `BlConversationKey` | `channel` + `externalUserId` |
| `BlEventKey` | `channel` + `eventId` |

HTTP does not call repositories. Managers do not call each other. The turns executor calls `IAssistantManager`.

## Types

**`BlConversationKey`:** `channel` (`web` | `telegram`) + `externalUserId`. No thread id. No linking channels.

**`BlEventKey`:** `channel` + `eventId`. One inbound delivery. Same `eventId` on another channel is a different delivery.

**`BlMessage`:** `role`, `text`, time, optional outcome kind, optional `BlStayOption[]`.

**`BlConversation`:** key + ordered messages.

**`BlTurnCommand`:** `BlConversationKey`, text, eventId. Inbound turn for HTTP. `eventId` is not nested as a second key; `BlEventKey` is `(conversation.channel, eventId)`.

**`BlTurnResult`:** `BlAssistantResponse` + `duplicate`. Built by the **turns executor**, not mapped from the DB.

**`BlProcessedEvent`:** `BlEventKey` + `BlAssistantResponse` outcome. No duplicate flag. `DbProcessedEvent` stays flattened (`channel`, `eventId`, outcome columns) and stores `DbStayOption[]`, not `BlStayOption`.

**`DbStayOption`:** persistence snapshot of a shown stay (`name`, `stayType`, `location`, `price`, `rating`, `why`, `url`). Not catalog `DbStayListing`.

**`DbConversation`:** key and `DbMessage` list only. `DbMessage` also stores `DbStayOption[]`.

## How the parts interact

Turns HTTP (`TurnsCommandExecutor`), not a domain wrapper:

```
TurnsCommandExecutor.execute(BlTurnCommand)
  eventKey = BlEventKey(command.conversation.channel, command.eventId)
  existing = IProcessedEventManager.getEvent(eventKey)
  if present → BlTurnResult(existing.outcome, duplicate=true)
  else:
    history = IConversationManager.getConversation(command.conversation)
    response = IAssistantManager.respond({ messages: history + this user, latestUserText })
    if IAssistantManager throws: response = fail process (retry)
    IConversationManager.saveTurn(command.conversation, command.text, response)
    IProcessedEventManager.save(BlProcessedEvent(eventKey, response))
    BlTurnResult(response, duplicate=false)
```

```
IConversationManager.getConversation(key)
  find → map to Bl, or empty conversation (not an error)

IConversationManager.saveTurn(key, text, response)
  1. validate key + text + response
  2. load conversation (or empty)
  3. append user BlMessage(text)
  4. append assistant BlMessage from response (kind, text, recommendations)
  5. save
```

Isolation: conversation repository keys by `BlConversationKey`. The manager never loads a second key in one turn. Follow-up is the same key, so “cheaper” still has “hotels in Lisbon” in `messages`.

## In-memory H2 behaviour (document in submission README)

H2 is an in-process SQL database (`jdbc:h2:mem:vacationary;MODE=PostgreSQL`). It dies on process restart, is not shared across instances, and is not durable. That is allowed (B-11, B-12). Stay listings live in the same database.

## Wiring

```
IProcessedEventManager ← ProcessedEventManager(IProcessedEventRepository, ProcessedEventMapper)
IConversationManager ← ConversationManager(IConversationRepository, ConversationMapper)
IConversationRepository ← JdbcConversationStore(DataSource)
IProcessedEventRepository ← JdbcProcessedEventStore(DataSource)
```

Conversation tests do not bind an assistant. Duplicate-event, follow-up prompt, and assistant-throw coverage belong on the turns executor.

## Swap

- New `IConversationRepository` (durable SQL) without changing the manager.

## Out of this domain

- Stay search, LLM SDKs, `IAssistantManager`
- `Api*` JSON, HTTP status
- A destination/filter snapshot beside the messages
- Account linking, security
