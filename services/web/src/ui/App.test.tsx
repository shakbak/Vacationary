import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { TransportError, type StayOption, type TurnResponse, type VacationaryApi } from "../api/types";
import type { Session } from "../session/session";
import { App } from "./App";

const session: Session = {
	getOrCreateUserId: () => "user-1",
	newEventId: () => "evt-1",
};

function option(name: string, stayType = "vacation_rental"): StayOption {
	return {
		name,
		stayType,
		location: "Trastevere, Rome",
		price: "$120 / night",
		rating: "4.6",
		why: "Fits this request.",
		url: `https://example.com/${name}`,
	};
}

function deferred<T>() {
	let resolve!: (value: T) => void;
	let reject!: (reason: unknown) => void;
	const promise = new Promise<T>((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, resolve, reject };
}

describe("App", () => {
	it("shows the empty state after history loads", async () => {
		const api: VacationaryApi = {
			getConversation: vi.fn().mockResolvedValue([]),
			postTurn: vi.fn(),
		};
		render(<App api={api} session={session} />);
		expect(await screen.findByText("I recommend places to stay.")).toBeInTheDocument();
		expect(screen.getByLabelText("Message")).toBeEnabled();
	});

	it("disables send during loading and shows in-transcript copy then five cards", async () => {
		const user = userEvent.setup();
		const turn = deferred<TurnResponse>();
		const api: VacationaryApi = {
			getConversation: vi.fn().mockResolvedValue([]),
			postTurn: vi.fn().mockReturnValue(turn.promise),
		};
		render(<App api={api} session={session} />);
		await screen.findByText("I recommend places to stay.");
		await user.type(screen.getByLabelText("Message"), "Find me a hotel in Rome");
		await user.click(screen.getByRole("button", { name: "Send" }));
		expect(screen.getByText("Finding stays…")).toBeInTheDocument();
		expect(screen.getByLabelText("Message")).toBeDisabled();
		expect(screen.getByRole("button", { name: "Send" })).toBeDisabled();
		turn.resolve({
			outcome: "recommend",
			text: [
				"Here are five lodging options for your trip.",
				"",
				"| Name | Why it stands out |",
				"|------|-------------------|",
				"| Alpha | Quiet courtyard |",
				"| Bravo | Near the station |",
				"| Charlie | Family kitchen |",
				"| Delta | Harbor views |",
				"| Echo | Quiet garden |",
			].join("\n"),
			recommendations: [
				option("Alpha"),
				option("Bravo"),
				option("Charlie"),
				option("Delta"),
				option("Echo"),
			],
			duplicate: false,
		});
		expect(await screen.findByText("Alpha")).toBeInTheDocument();
		expect(screen.getByText("Here are five lodging options for your trip.")).toBeInTheDocument();
		expect(screen.queryByText("Why it stands out")).not.toBeInTheDocument();
		expect(screen.getByText("Quiet courtyard")).toBeInTheDocument();
		expect(screen.queryByText("Fits this request.")).not.toBeInTheDocument();
		expect(screen.getAllByRole("link", { name: "View listing" })).toHaveLength(5);
		expect(screen.getAllByText("Vacation rental")).toHaveLength(5);
		expect(screen.queryByText("Finding stays…")).not.toBeInTheDocument();
		await waitFor(() => expect(screen.getByLabelText("Message")).toHaveFocus());
	});

	it("treats clarify as an ordinary reply, fail as transcript error, HTTP as status with draft", async () => {
		const user = userEvent.setup();
		const api: VacationaryApi = {
			getConversation: vi.fn().mockResolvedValue([]),
			postTurn: vi
				.fn()
				.mockResolvedValueOnce({
					outcome: "clarify",
					text: "Where would you like to stay?",
					recommendations: [],
					duplicate: false,
				} satisfies TurnResponse)
				.mockResolvedValueOnce({
					outcome: "fail",
					text: "I could not process that message. Please retry.",
					recommendations: [],
					duplicate: false,
				} satisfies TurnResponse)
				.mockRejectedValueOnce(new TransportError("Could not reach the assistant. Please retry.")),
		};
		render(<App api={api} session={session} />);
		await screen.findByText("I recommend places to stay.");
		await user.type(screen.getByLabelText("Message"), "a hotel");
		await user.click(screen.getByRole("button", { name: "Send" }));
		expect(await screen.findByText("Where would you like to stay?")).toBeInTheDocument();
		expect(screen.queryByText("Could not reach the assistant. Please retry.")).not.toBeInTheDocument();
		expect(screen.queryByText("Search could not finish.")).not.toBeInTheDocument();

		await user.type(screen.getByLabelText("Message"), "Rome");
		await user.click(screen.getByRole("button", { name: "Send" }));
		expect(await screen.findByText("I could not process that message. Please retry.")).toBeInTheDocument();
		expect(screen.getByText("Search could not finish.")).toBeInTheDocument();

		await user.type(screen.getByLabelText("Message"), "try again");
		await user.click(screen.getByRole("button", { name: "Send" }));
		expect(await screen.findByText("Could not reach the assistant. Please retry.")).toBeInTheDocument();
		expect(screen.getByLabelText("Message")).toHaveValue("try again");
		expect(screen.getByLabelText("Message")).toBeEnabled();
	});
});
