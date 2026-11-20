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

Set `VACATIONARY_LLM_API_KEY` for a real LLM. Tests use a fake client and do not call the network.
