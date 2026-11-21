import {
	type HistoryMessage,
	type StayOption,
	type TurnOutcome,
	type TurnRequest,
	type TurnResponse,
	type VacationaryApi,
	TransportError,
} from "./types";

const FALLBACK_HTTP_ERROR = "Could not reach the assistant. Please retry.";

const OUTCOMES: ReadonlySet<string> = new Set(["recommend", "clarify", "refuse_scope", "fail"]);

function apiBase(): string {
	const base = import.meta.env.VITE_API_BASE_URL;
	return base == null || base === "" ? "" : base.replace(/\/$/, "");
}

function asStayOptions(value: unknown): StayOption[] {
	if (!Array.isArray(value)) {
		return [];
	}
	const options: StayOption[] = [];
	for (const row of value) {
		if (row == null || typeof row !== "object") {
			continue;
		}
		const option = row as Partial<StayOption>;
		options.push({
			name: String(option.name ?? ""),
			stayType: String(option.stayType ?? ""),
			location: String(option.location ?? ""),
			price: String(option.price ?? ""),
			rating: String(option.rating ?? ""),
			why: String(option.why ?? ""),
			url: String(option.url ?? ""),
		});
	}
	return options;
}

function asOutcome(value: unknown): TurnOutcome | null {
	return typeof value === "string" && OUTCOMES.has(value) ? (value as TurnOutcome) : null;
}

async function readJson(response: Response): Promise<unknown> {
	const text = await response.text();
	if (text === "") {
		return null;
	}
	try {
		return JSON.parse(text);
	} catch {
		throw new TransportError(FALLBACK_HTTP_ERROR, response.status);
	}
}

function transportFromBody(status: number, body: unknown): TransportError {
	if (body != null && typeof body === "object") {
		const record = body as { message?: unknown; code?: unknown };
		const message = typeof record.message === "string" && record.message !== ""
			? record.message
			: FALLBACK_HTTP_ERROR;
		const code = typeof record.code === "string" ? record.code : null;
		return new TransportError(message, status, code);
	}
	return new TransportError(FALLBACK_HTTP_ERROR, status);
}

function asHistoryMessages(body: unknown): HistoryMessage[] {
	if (body == null || typeof body !== "object" || !("messages" in body)) {
		return [];
	}
	const raw = (body as { messages: unknown }).messages;
	if (!Array.isArray(raw)) {
		return [];
	}
	const messages: HistoryMessage[] = [];
	for (const row of raw) {
		if (row == null || typeof row !== "object") {
			continue;
		}
		const item = row as {
			role?: unknown;
			text?: unknown;
			recommendations?: unknown;
			outcome?: unknown;
		};
		const role = item.role === "assistant" ? "assistant" : "user";
		messages.push({
			role,
			text: String(item.text ?? ""),
			recommendations: asStayOptions(item.recommendations),
			outcome: asOutcome(item.outcome),
		});
	}
	return messages;
}

function asTurnResponse(body: unknown): TurnResponse {
	if (body == null || typeof body !== "object") {
		throw new TransportError(FALLBACK_HTTP_ERROR, 200);
	}
	const record = body as {
		outcome?: unknown;
		text?: unknown;
		recommendations?: unknown;
		duplicate?: unknown;
	};
	const outcome = asOutcome(record.outcome);
	if (outcome == null) {
		throw new TransportError(FALLBACK_HTTP_ERROR, 200);
	}
	return {
		outcome,
		text: String(record.text ?? ""),
		recommendations: asStayOptions(record.recommendations),
		duplicate: record.duplicate === true,
	};
}

export function createApi(baseUrl: string = apiBase()): VacationaryApi {
	return {
		async getConversation(userId: string) {
			let response: Response;
			try {
				response = await fetch(
					`${baseUrl}/conversations/web/${encodeURIComponent(userId)}`,
				);
			} catch {
				throw new TransportError(FALLBACK_HTTP_ERROR);
			}
			const body = await readJson(response);
			if (!response.ok) {
				throw transportFromBody(response.status, body);
			}
			return asHistoryMessages(body);
		},
		async postTurn(request: TurnRequest) {
			let response: Response;
			try {
				response = await fetch(`${baseUrl}/turns`, {
					method: "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify(request),
				});
			} catch {
				throw new TransportError(FALLBACK_HTTP_ERROR);
			}
			const body = await readJson(response);
			if (!response.ok) {
				throw transportFromBody(response.status, body);
			}
			return asTurnResponse(body);
		},
	};
}

export const backendApi = createApi();
