import { describe, expect, it } from "vitest";

import { stripMarkdownTables } from "./stripReply";

describe("stripMarkdownTables", () => {
	it("keeps the intro and drops markdown table rows", () => {
		const text = [
			"Here are five lodging options in Rome:",
			"",
			"| Name | Type | Why it fits |",
			"|------|------|-------------|",
			"| Alpha Inn | Hotel | Quiet courtyard |",
			"| Bravo House | Hostel | Near the station |",
		].join("\n");
		expect(stripMarkdownTables(text)).toBe("Here are five lodging options in Rome:");
		expect(stripMarkdownTables(text)).not.toContain("Why it fits");
		expect(stripMarkdownTables(text)).not.toContain("Alpha Inn");
	});

	it("drops numbered stay dumps", () => {
		const numbered =
			"1. **Hotel Campo de' Fiori** – HOTEL – $210 / night – *Why:* Quiet courtyard. – [Booking.com](https://example.com)";
		expect(stripMarkdownTables(`Intro\n\n${numbered}`)).toBe("Intro");
	});
});
