import { describe, expect, it } from "vitest";

import { stayTypeLabel } from "./stayType";

describe("stayTypeLabel", () => {
	it("maps backend enums to ordinary words", () => {
		expect(stayTypeLabel("vacation_rental")).toBe("Vacation rental");
		expect(stayTypeLabel("BNB")).toBe("B&B");
		expect(stayTypeLabel("hotel")).toBe("Hotel");
	});
});
