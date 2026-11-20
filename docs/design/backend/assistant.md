# Assistant domain — inner design

Single lodging agent. Manager depends on other managers/providers, not on HTTP.

Collaborators: `IStayManager` (recommendation) and `ILlmManager` (prompt + LLM). Implement after recommendation.

Follow-ups use **the message list**, not a stored destination or filter object. If the user said “hotels in Lisbon” and then “cheaper”, both lines are in `messages`; the model must put Lisbon (and any other filters still in force) on the tool call. Nothing is merged from a side snapshot.

## Inner parts

```
AssistantManager                 implements IAssistantManager
  ├── ILlmManager                prompt + complete (decide, then reply after tool)
  │     ├── ILlmPromptBuilder    history → BlLlmRequest (incl. tool schema)
  │     └── ILlmClient           infra: complete()
  │           └── IOpenAiChatMapper   BlLlmRequest ↔ OpenAI JSON
  ├── StaySearchParser           tool JSON → BlStaySearchQuery
  ├── IStayManager               search(query)
  └── AssistantResponseMapper     BlLlmResponse + optional 5 options → BlAssistantResponse
```

| Part | Responsibility |
|---|---|
| `IAssistantManager` | `respond(BlAssistantRequest) → BlAssistantResponse` |
| `AssistantManager` | One-tool loop: decide → maybe search once → write reply |
| `ILlmManager` | `complete(request)` then `completeWithToolResult(...)`; Assistant never talks to the LLM client |
| `ILlmPromptBuilder` | System + **this conversation’s** messages; tells the model to reuse destination/filters already stated in those messages when calling the tool |
| `ILlmClient` | `complete(BlLlmRequest) → BlLlmResponse` |
| `IOpenAiChatMapper` | Serialize `BlLlmRequest` to OpenAI JSON and parse the raw reply |
| `StaySearchParser` | Parses **this tool call’s JSON args only** into a query, missing destination, or invalid arguments |
| `AssistantResponseMapper` | Copies **tool** `BlStayOption`s into `recommend`; never invents rows |
| `BlAssistantRequest` / `BlAssistantResponse` | Conversation ↔ assistant |
| `BlLlmRequest` / `BlLlmResponse` | `text` \| `toolRequest` \| `error` |
| `BlAssistantException` | Unexpected; conversation maps this to `fail` |

Conversation never sees `ILlmManager` or `ILlmClient`. HTTP never sees `IStayManager`.

## Types

**`BlAssistantRequest`:** `messages` for this conversation, `latestUserText`. No extra destination field.

**`BlAssistantResponse.kind`:** `clarify` | `refuse_scope` | `recommend` | `fail`.  
`recommend` carries exactly the 5 `BlStayOption`s from `IStayManager`. `text` is always English.

**`BlLlmResponse`:** `text` | `toolRequest(name, jsonArgs)` | `error`.

## How the parts interact

One-tool loop. First `complete` **decides**. A `ToolRequest` runs `stay_search` at most once. Second `complete` only **writes the English reply** (no tools). It cannot change the outcome kind.

```
AssistantManager.respond(request)
  completion  = ILlmManager.complete(request)   // build this conversation’s messages + first complete

  if error → fail process (retry). No stays.
  if text  → fromModelText: clarify | refuse_scope. No stays.

  if toolRequest stay_search (once):
    parse args only (no hidden destination)
    missing destination → clarify
    invalid args / search fail → fail search
    else:
      reply = ILlmManager.completeWithToolResult(...)   // write reply; ignore another tool
      recommend(reply text or fallback sentence, those 5 options)
```

No second tool type. A failed/blank second `complete` still recommends the 5.

### Rules the **manager** enforces (not only the prompt)

English; refuse non-lodging; one destination question; optional people/dates/budget never block; never emit five hotels on fail.

The prompt must say: when the user follows up, take destination and filters from **earlier messages in this conversation** and pass them on the tool call. `StaySearchParser` does not look up a previous destination itself.

## Wiring

```
IAssistantManager ← AssistantManager(ILlmManager, StaySearchParser, IStayManager, AssistantResponseMapper)
ILlmManager ← LlmManager(ILlmPromptBuilder, ILlmClient)
ILlmClient ← HttpLlmClient(IOpenAiChatMapper)   or   FakeLlmClient (tests)
IOpenAiChatMapper ← OpenAiChatMapper
```

## Swap

- `FakeLlmClient` for assistant/conversation tests (scripted text / toolRequest / error).
- Fake `IStayManager` when testing the assistant without the repository.

## Out of this domain

- Persistence, HTTP JSON, WhatsApp
- A stored destination/filter snapshot beside the messages
- Extra tools, streaming, room-type schema (nice to have)
