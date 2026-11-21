import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { TransportError, type TurnResponse, type VacationaryApi } from "../api/types";
import type { Session } from "../session/session";
import { useChat } from "./useChat";

const session: Session = {
	getOrCreateUserId: () => "user-1",
	newEventId: () => "evt-1",
};

function deferred<T>() {
	let resolve!: (value: T) => void;
	let reject!: (reason: unknown) => void;
	const promise = new Promise<T>((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, resolve, reject };
}

describe("useChat", () => {
	it("loads empty history and ignores blank send", async () => {
		const api: VacationaryApi = {
			getConversation: vi.fn().mockResolvedValue([]),
			postTurn: vi.fn(),
		};
		const { result } = renderHook(() => useChat(api, session));
		await waitFor(() => expect(result.current.loadingHistory).toBe(false));
		expect(result.current.messages).toEqual([]);
		await act(async () => {
			await result.current.sendTurn("   ");
		});
		expect(api.postTurn).not.toHaveBeenCalled();
	});

	it("locks send while a turn is in flight then commits recommend", async () => {
		const turn = deferred<TurnResponse>();
		const api: VacationaryApi = {
			getConversation: vi.fn().mockResolvedValue([]),
			postTurn: vi.fn().mockReturnValue(turn.promise),
		};
		const { result } = renderHook(() => useChat(api, session));
		await waitFor(() => expect(result.current.loadingHistory).toBe(false));
		let sendPromise: Promise<void> = Promise.resolve();
		act(() => {
			sendPromise = result.current.sendTurn("Rome");
		});
		await waitFor(() => expect(result.current.sending).toBe(true));
		expect(result.current.pendingText).toBe("Rome");
		turn.resolve({
			outcome: "recommend",
			text: "Here are five lodging options for your trip.",
			recommendations: [
				{
					name: "Alpha",
					stayType: "hotel",
					location: "Rome",
					price: "$100 / night",
					rating: "4.5",
					why: "why",
					url: "https://example.com/a",
				},
			],
			duplicate: false,
		});
		await act(async () => {
			await sendPromise;
		});
		expect(result.current.sending).toBe(false);
		expect(result.current.messages).toHaveLength(2);
		expect(result.current.messages[1]?.outcome).toBe("recommend");
		expect(result.current.draft).toBe("");
	});

	it("treats 200 fail as a saved assistant error and restores draft on HTTP error", async () => {
		const api: VacationaryApi = {
			getConversation: vi.fn().mockResolvedValue([]),
			postTurn: vi
				.fn()
				.mockResolvedValueOnce({
					outcome: "fail",
					text: "I could not process that message. Please retry.",
					recommendations: [],
					duplicate: false,
				} satisfies TurnResponse)
				.mockRejectedValueOnce(new TransportError("Could not reach the assistant. Please retry.", 500)),
		};
		const { result } = renderHook(() => useChat(api, session));
		await waitFor(() => expect(result.current.loadingHistory).toBe(false));
		await act(async () => {
			await result.current.sendTurn("Rome");
		});
		expect(result.current.httpError).toBeNull();
		expect(result.current.messages[1]?.outcome).toBe("fail");
		expect(result.current.draft).toBe("");
		await act(async () => {
			await result.current.sendTurn("Rome again");
		});
		expect(result.current.httpError).toBe("Could not reach the assistant. Please retry.");
		expect(result.current.draft).toBe("Rome again");
		expect(result.current.messages).toHaveLength(2);
	});

	it("starts empty with a note when history fails", async () => {
		const api: VacationaryApi = {
			getConversation: vi.fn().mockRejectedValue(new TransportError("down", 500)),
			postTurn: vi.fn(),
		};
		const { result } = renderHook(() => useChat(api, session));
		await waitFor(() => expect(result.current.loadingHistory).toBe(false));
		expect(result.current.messages).toEqual([]);
		expect(result.current.historyNote).toMatch(/could not be loaded/i);
	});
});
