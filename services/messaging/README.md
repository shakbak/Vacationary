# Messaging platforms service

Separate Java process for **WhatsApp only**. It receives Cloud API webhooks, calls the backend `POST /turns`, and sends the same assistant outcome back as English WhatsApp text. It does not run the LLM or search stays.

Design: [docs/design/messaging](../../docs/design/messaging/overview.md)

Requirements: [docs/requirements/messaging.md](../../docs/requirements/messaging.md)

## Why 1:1 WhatsApp

Travel help is a private chat with one traveler. WhatsApp maps one phone number to one user, which is the backend conversation key (`whatsapp` + `wa_id`). Groups and broadcasts are out of scope.

## Run

Needs JDK 21+. Start the **backend** on port 8080 first (`VACATIONARY_LLM_API_KEY` or repo-root `sec.txt`). Then from this directory:

```bash
# Windows: mvnw.cmd
export VACATIONARY_WHATSAPP_VERIFY_TOKEN=your-verify-token
export VACATIONARY_WHATSAPP_ACCESS_TOKEN=your-cloud-api-token
export VACATIONARY_WHATSAPP_PHONE_NUMBER_ID=your-phone-number-id
./mvnw spring-boot:run
```

Listens on **8081**. Tests: `./mvnw test` (no WhatsApp or backend network).

## Talk to the bot (reviewers)

1. In Meta for Developers, add the **WhatsApp** product to an app and open **API Setup**.
2. Copy the temporary (or system-user) **access token** and **Phone number ID**. Add a recipient number you can message from.
3. Expose this service with a public HTTPS URL (for local: `ngrok http 8081`).
4. Set the WhatsApp webhook callback to `https://<host>/webhooks/whatsapp` with the same verify token as `VACATIONARY_WHATSAPP_VERIFY_TOKEN`. Subscribe to **messages**.
5. Message the test business number in **English**. A city is enough (`Hotels in Barcelona`). Dates, budget, and party size are optional.

You should get one WhatsApp reply from the shared assistant: a clarification question if no city, or five stays with type, area, price, rating, why, and a tappable OTA link. English only.

If the backend restarts, in-memory conversations are wiped (same limit as web). If WhatsApp delivery fails after the assistant already ran, a webhook retry will not send a second copy (backend duplicate id).

## Limits

- No Telegram/Slack adapters in v1.
- No buttons, lists, or media-as-search.
- Web chat does not go through this service.
