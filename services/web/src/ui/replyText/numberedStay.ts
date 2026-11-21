import { stripMarkup } from "./markup";

const NUMBERED_WHY =
	/^\s*\d+\.\s+(?:\*\*(.+?)\*\*|(.+?))\s+[–—-].*?(?:\*[Ww]hy:\*|\bWhy:)\s*(.+)$/;
const TRAILING_MARKDOWN_LINK = /\s*[–—-]?\s*\[[^\]]*]\([^)]*\)\s*$/;

export function parseNumberedWhyPairs(text: string): Array<[string, string]> {
	const pairs: Array<[string, string]> = [];
	for (const line of text.split(/\r?\n/)) {
		const match = NUMBERED_WHY.exec(line);
		if (match == null) {
			continue;
		}
		const name = stripMarkup(match[1] ?? match[2] ?? "");
		const why = stripMarkup(match[3] ?? "").replace(TRAILING_MARKDOWN_LINK, "").trim();
		if (name !== "" && why !== "") {
			pairs.push([name, why]);
		}
	}
	return pairs;
}

export function isStayDumpLine(line: string): boolean {
	const trimmed = line.trim();
	if (!/^\d+\.\s+/.test(trimmed)) {
		return false;
	}
	return /\*Why:\*|\bWhy:|\/ night|\bRating\b/i.test(trimmed);
}
