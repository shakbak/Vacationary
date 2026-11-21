export const USER_ID_KEY = "vacationary.web.userId";

export type Session = {
	getOrCreateUserId(): string;
	newEventId(): string;
};

function randomId(): string {
	return crypto.randomUUID();
}

export const browserSession: Session = {
	getOrCreateUserId() {
		const existing = sessionStorage.getItem(USER_ID_KEY);
		if (existing != null && existing !== "") {
			return existing;
		}
		const created = randomId();
		sessionStorage.setItem(USER_ID_KEY, created);
		return created;
	},
	newEventId() {
		return randomId();
	},
};
