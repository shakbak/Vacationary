# Backend

Java Spring service for conversations, the assistant, and lodging search.

Recommendation and assistant run in-process. Conversations, processed events, and the stay catalog live in **in-process H2** (`MODE=PostgreSQL`): they are lost on restart, are not shared across instances, and are not durable. That is allowed for the submission.

Requirements: [docs/requirements/backend.md](../../docs/requirements/backend.md)

Design: [docs/design/backend](../../docs/design/backend/overview.md)

Needs JDK 21+ (`JAVA_HOME` should point at a JDK 21 install).

```bash
./mvnw test
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

The default LLM is **Groq** (OpenAI-compatible). Put the Groq key in a `sec.txt` file at the repo root (gitignored) or set `VACATIONARY_LLM_API_KEY`. Default model is `openai/gpt-oss-20b` (Llama 3.3 70B is no longer on the free tier). Override URL/model with `VACATIONARY_LLM_BASE_URL` and `VACATIONARY_LLM_MODEL` if needed. Tests use a fake client and do not call the network.
