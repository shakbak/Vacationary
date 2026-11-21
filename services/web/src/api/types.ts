export type TurnOutcome = "recommend" | "clarify" | "refuse_scope" | "fail";

export type StayOption = {
	name: string;
	stayType: string;
	location: string;
	price: string;
	rating: string;
	why: string;
	url: string;
};

export type TurnRequest = {
	channel: "web";
	externalUserId: string;
	text: string;
	eventId: string;
};

export type TurnResponse = {
	outcome: TurnOutcome;
	text: string;
	recommendations: StayOption[];
	duplicate: boolean;
};

export type HistoryMessage = {
	role: "user" | "assistant";
	text: string;
	recommendations: StayOption[];
	outcome?: TurnOutcome | null;
};

export type ApiErrorBody = {
	code: string;
	message: string;
};

export class TransportError extends Error {
	readonly status: number | null;
	readonly code: string | null;

	constructor(message: string, status: number | null = null, code: string | null = null) {
		super(message);
		this.name = "TransportError";
		this.status = status;
		this.code = code;
	}
}

export type VacationaryApi = {
	getConversation(userId: string): Promise<HistoryMessage[]>;
	postTurn(request: TurnRequest): Promise<TurnResponse>;
};
