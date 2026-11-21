import { afterEach, describe, expect, it } from "vitest";

import { USER_ID_KEY, browserSession } from "./session";

describe("browserSession", () => {
	afterEach(() => {
		sessionStorage.clear();
	});

	it("creates a user id and reuses it", () => {
		const first = browserSession.getOrCreateUserId();
		const second = browserSession.getOrCreateUserId();
		expect(first).toMatch(
			/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
		);
		expect(second).toBe(first);
		expect(sessionStorage.getItem(USER_ID_KEY)).toBe(first);
	});

	it("issues a new event id each time", () => {
		expect(browserSession.newEventId()).not.toBe(browserSession.newEventId());
	});
});
