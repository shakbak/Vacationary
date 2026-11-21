import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { StayCard } from "./StayCard";

describe("StayCard", () => {
	it("toggles a long why between see more and see less", async () => {
		const user = userEvent.setup();
		Object.defineProperty(HTMLElement.prototype, "clientHeight", {
			configurable: true,
			get() {
				return this.className.includes("stay-card__why--clamp") ? 40 : 120;
			},
		});
		Object.defineProperty(HTMLElement.prototype, "scrollHeight", {
			configurable: true,
			get() {
				return 120;
			},
		});
		render(
			<StayCard
				option={{
					name: "Alpha Inn",
					stayType: "hotel",
					location: "Rome",
					price: "$100 / night",
					rating: "4.5",
					why: "A quiet courtyard hotel near the river, good for a four-night stay, with breakfast and a walk to the old town.",
					url: "https://example.com/alpha",
				}}
			/>,
		);
		expect(screen.getByText(/quiet courtyard hotel/i)).toBeInTheDocument();
		const more = await screen.findByRole("button", { name: "See more" });
		await user.click(more);
		expect(screen.getByRole("button", { name: "See less" })).toBeInTheDocument();
		await user.click(screen.getByRole("button", { name: "See less" }));
		expect(screen.getByRole("button", { name: "See more" })).toBeInTheDocument();
	});
});
