import { describe, expect, it } from "vitest";

import { whyForStay } from "./whyForStay";

describe("whyForStay", () => {
	it("reads Why it stands out from the table by stay name", () => {
		const text = [
			"Here are five lodging options in Rome:",
			"| Name | Type | Why it stands out |",
			"|------|------|-------------------|",
			"| Alpha Inn | Hotel | Quiet courtyard near the Pantheon |",
			"| Bravo House | Hostel | Easy walk to Termini |",
		].join("\n");
		expect(whyForStay(text, "Alpha Inn")).toBe("Quiet courtyard near the Pantheon");
		expect(whyForStay(text, "The Bravo House")).toBe("Easy walk to Termini");
		expect(whyForStay(text, "Unknown Lodge")).toBeNull();
	});

	it("reads a Stay column and a numbered Why dump", () => {
		const table = [
			"Stay | Why it stands out",
			"---|---",
			"Hotel Campo de' Fiori | Quiet courtyard a short walk from the market",
		].join("\n");
		expect(whyForStay(table, "Hotel Campo de' Fiori")).toBe(
			"Quiet courtyard a short walk from the market",
		);
		const numbered =
			"1. **Hotel Campo de' Fiori** – HOTEL – $210 / night – *Why:* Quiet courtyard a short walk from the market. – [Booking.com](https://example.com)";
		expect(whyForStay(numbered, "Hotel Campo de' Fiori")).toBe(
			"Quiet courtyard a short walk from the market.",
		);
	});
});
