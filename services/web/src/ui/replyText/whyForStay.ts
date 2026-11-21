import { parseTableWhyPairs } from "./markdownTable";
import { nameMatchScore, normalizeName } from "./names";
import { parseNumberedWhyPairs } from "./numberedStay";

export function whyForStay(replyText: string, stayName: string): string | null {
	const wantedName = normalizeName(stayName);
	if (wantedName === "") {
		return null;
	}
	let bestWhy: string | null = null;
	let bestScore = 0;
	for (const [name, why] of whyPairs(replyText)) {
		const score = nameMatchScore(wantedName, normalizeName(name));
		if (score > bestScore) {
			bestScore = score;
			bestWhy = why;
		}
	}
	return bestWhy;
}

function whyPairs(text: string): Array<[string, string]> {
	return [...parseTableWhyPairs(text), ...parseNumberedWhyPairs(text)];
}
