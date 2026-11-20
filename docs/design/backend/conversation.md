# Conversation domain — inner design

Isolation, history, idempotency, turn orchestration. **Manager + repositories + Db mappers + in-memory stubs.** The manager calls `IAssistantManager` with the message list for one key.

Implement after assistant.

The store keeps **messages**, not a destination or filter snapshot. Follow-up context is those messages.

## Inner parts

```
ConversationManager                 implements ConversationManager port
  ├── ConversationRepository        history for one ConversationKey
  │     └── ConversationMapper      DbConversation ↔ BlConversation
  ├── ProcessedEventRepository      eventId → BlTurnResult
  │     └── mapper                  stored record ↔ BlTurnResult
  └── IAssistantManager                    other domain (interface)
```

| Part | Responsibility |
|---|---|
| `ConversationManager` | `handleTurn`, `getConversation`; **the only port HTTP uses** |
| `ConversationRepository` | `find(key)`, `save(conversation)` |
| `ProcessedEventRepository` | `find(channel, eventId)`, `save(...)` |
| `InMemoryConversationStore` | `Map<ConversationKey, DbConversation>` |
| `InMemoryProcessedEventStore` | `Map<(channel,eventId), row>` |
| `DbConversation` / `DbMessage` | Persistence shape: key + messages |
| `ConversationMapper` | `DbConversation` ↔ `BlConversation` |
| `BlConversation`, `BlMessage`, `BlTurnCommand`, `BlTurnResult` | Manager language |
| `BlConversationKey` | `channel` + `externalUserId` |

HTTP and assistant do not touch repositories. Repositories do not call `IAssistantManager`.

## Types

**`BlConversationKey`:** `channel` (`web` | `whatsapp`) + `externalUserId`. No thread id. No linking channels.

**`BlMessage`:** `role`, `text`, time, optional outcome kind, optional `BlStayOption[]`.

**`BlConversation`:** key + ordered messages.

**`BlTurnCommand`:** channel, externalUserId, text, eventId.

**`BlTurnResult`:** `BlAssistantResponse` + `duplicate`.

**`DbConversation`:** key and `DbMessage` list only.

## How the parts interact

```
ConversationManager.handleTurn(BlTurnCommand)
  1. hit = ProcessedEventRepository.find(channel, eventId)
     if hit: return hit as duplicate=true
        // no save, no Assistant
  2. key = (channel, externalUserId)
     row = ConversationRepository.find(key)
     conv = row mapped to BlConversation, or new empty BlConversation(key)
  3. append user BlMessage(text)
  4.      response = IAssistantManager.respond({
       messages: conv.messages,          // this key only, including prior turns
       latestUserText: text
     })
     if IAssistantManager throws: response = fail("could not process, retry")
  5. append assistant BlMessage from response
  6. ConversationRepository.save(mapper.toDb(conv))
  7. result = BlTurnResult(response, duplicate=false)
     ProcessedEventRepository.save(channel, eventId, result)
  8. return result

ConversationManager.getConversation(key)
  find → map to Bl, or empty conversation (not an error)
```

Isolation: repositories key by `BlConversationKey`. The manager never loads a second key in one turn. Follow-up is the same key, so “cheaper” still has “hotels in Lisbon” in `messages`.

## In-memory stub behaviour (document in submission README)

Both stub maps die on process restart, are not shared across instances, and are not durable. That is allowed (B-11, B-12).

## Wiring

```
ConversationManager ← ConversationManagerImpl(ConversationRepository, ProcessedEventRepository, IAssistantManager)
ConversationRepository ← InMemoryConversationStore + ConversationMapper
ProcessedEventRepository ← InMemoryProcessedEventStore
```

Conversation tests bind a **fake IAssistantManager** (scripted outcomes) so isolation and duplicate-event tests do not need an LLM.

## Swap

- New `ConversationRepository` (DB) without changing the manager.
- Fake `IAssistantManager` for this domain’s tests.

## Out of this domain

- Stay search, LLM SDKs
- `Api*` JSON, HTTP status
- A destination/filter snapshot beside the messages
- Account linking, security
