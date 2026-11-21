import { afterEach, describe, expect, it, vi } from "vitest";

import { createApi } from "./client";
import { TransportError } from "./types";

describe("createApi", () => {
	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it("posts a turn and reads a 200 fail outcome as success", async () => {
		const fetchMock = vi.fn().mockResolvedValue(
			new Response(
				JSON.stringify({
					outcome: "fail",
					text: "Search did not work. Please retry or rephrase.",
					recommendations: [],
					duplicate: false,
				}),
				{ status: 200, headers: { "Content-Type": "application/json" } },
			),
		);
		vi.stubGlobal("fetch", fetchMock);
		const api = createApi("");
		const result = await api.postTurn({
			channel: "web",
			externalUserId: "u1",
			text: "Rome",
			eventId: "e1",
		});
		expect(result.outcome).toBe("fail");
		expect(result.recommendations).toEqual([]);
		expect(fetchMock).toHaveBeenCalledWith("/turns", expect.objectContaining({ method: "POST" }));
	});

	it("throws TransportError on 500", async () => {
		vi.stubGlobal(
			"fetch",
			vi.fn().mockResolvedValue(
				new Response(JSON.stringify({ code: "INTERNAL_ERROR", message: "An unexpected error occurred" }), {
					status: 500,
					headers: { "Content-Type": "application/json" },
				}),
			),
		);
		const api = createApi("");
		await expect(api.getConversation("u1")).rejects.toMatchObject({
			name: "TransportError",
			message: "An unexpected error occurred",
			status: 500,
		} satisfies Partial<TransportError>);
	});
});
