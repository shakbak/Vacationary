# HTTP adapter — inner design

Pipeline: **validate → map API→BL → execute → map BL→API**, plus an error handler. Two features (`turns`, `history`). No rate limiter, no auth, no audit logger.

Depends on **`IProcessedEventManager`, `IConversationManager`, and `IAssistantManager` (turns)** and **`IConversationManager` (history)**. Implement last. Does not call stay search. The turns executor orchestrates duplicate short-circuit vs load history → assistant → save turn → save event.

## Inner parts

```
Spring controller / router
  └── RouteHandler.handle(req, res)
        1. SchemaValidator.validate
        2. RequestMapper.mapApiToBl
        3. CommandExecutor.execute        → getEvent, else assistant + save turn + save event
                                          → or getConversation
        4. ResponseHandler.handle         Bl → Api JSON
        catch → ErrorHandler.handle
```

| Part | Responsibility |
|---|---|
| `RouteHandler` | Shared pipeline; no lodging logic |
| `TurnsSchemaValidator` | Required JSON fields for POST `/turns` |
| `TurnsRequestMapper` | `ApiTurnRequest` → `BlTurnCommand` |
| `TurnsCommandExecutor` | `getEvent` → duplicate `BlTurnResult`; else load history, `assistant.respond`, `saveTurn`, save event |
| `TurnsResponseHandler` | `BlTurnResult` → `ApiTurnResponse` (status 200) |
| `HistoryRequestMapper` | path params → `BlConversationKey` |
| `HistoryCommandExecutor` | `conversationManager.getConversation` |
| `HistoryResponseHandler` | `BlConversation` → `ApiConversationResponse` |
| `HttpErrorHandler` | BL / validation → JSON; LLM/tool `fail` is a **200 body**, not a thrown error |
| Feature factories | Construct the five pieces per route |
| `Api*` DTOs | JSON only |

Generic `RouteHandler` lives under `http/spring/` once. Features live under `http/features/turns/` and `http/features/history/`. Factories under `http/factories/`.

A Spring `@RestController` is a thin entry that **delegates to `RouteHandler`**. It does not call the manager itself.

## API JSON (contract)

Port 8080. No auth.

### POST `/turns`

**`ApiTurnRequest`:** `channel`, `externalUserId`, `text`, `eventId`.

**`ApiTurnResponse`:** `outcome` (`recommend` \| `clarify` \| `refuse_scope` \| `fail`), `text`, `recommendations[]` (only for recommend; fields of `BlStayOption`), `duplicate`.

Missing/invalid body → 400 from validator/error handler, executor not called.

Processed turn, including `outcome: fail` and duplicates → **200**. LLM/search failure is a BL outcome, not an empty 5xx.

### GET `/conversations/{channel}/{externalUserId}`

**`ApiConversationResponse`:** ordered messages (`role`, `text`, optional recommendations, optional `outcome` on assistant turns). Unknown key → 200 empty list.

## How the parts interact (POST `/turns`)

```
RouteHandler
  SchemaValidator: channel, externalUserId, text, eventId present
  TurnsRequestMapper: ApiTurnRequest → BlTurnCommand
       (channel lowercased; no Telegram fields)
  TurnsCommandExecutor.execute(bl):
       eventKey = BlEventKey(bl.conversation.channel, bl.eventId)
       existing = IProcessedEventManager.getEvent(eventKey)
       if present → BlTurnResult(outcome, duplicate=true)
       else:
         history = IConversationManager.getConversation(bl.conversation)
         response = IAssistantManager.respond(history + this user)
         if assistant throws → fail process (retry)
         IConversationManager.saveTurn(bl.conversation, bl.text, response)
         IProcessedEventManager.save(BlProcessedEvent(eventKey, response))
         BlTurnResult(response, duplicate=false)
  TurnsResponseHandler:
       HTTP 200
       ApiTurnResponse from BlTurnResult
       StayOptions mapped field-for-field
  ErrorHandler:
       validation → 400
       unexpected → 500 JSON, not a fake hotel list
```

GET `/conversations/...` is the same pipeline with history mapper/executor/handler.

## Wiring

```
TurnsRouteHandler ← RouteHandler(
  TurnsSchemaValidator,
  TurnsRequestMapper,
  TurnsCommandExecutor(IProcessedEventManager, IConversationManager, IAssistantManager),
  TurnsResponseHandler,
  HttpErrorHandler)
```

Same pattern for history. Bootstrap registers those beans.

## Out of this adapter

- Calling `IStayManager`, `ILlmManager`, or `ILlmClient`
- Assembling `BlMessage` rows (conversation `saveTurn` does that)
- Telegram Bot API, buttons, signatures
- Login, rate limits, streaming
