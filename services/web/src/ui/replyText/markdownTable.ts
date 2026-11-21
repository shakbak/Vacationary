import { stripMarkup } from "./markup";

const NAME_HEADERS = ["name", "stay", "hotel", "property", "listing"];
const WHY_HEADERS = ["why", "stand", "highlight", "fit"];

export function parseTableWhyPairs(text: string): Array<[string, string]> {
	const rows = tableRows(text);
	if (rows.length < 2) {
		return [];
	}
	const header = rows[0].map((cell) => cell.toLowerCase());
	let nameIndex = findHeader(header, NAME_HEADERS);
	const whyIndex = findHeader(header, WHY_HEADERS);
	if (whyIndex < 0) {
		return [];
	}
	if (nameIndex < 0) {
		nameIndex = 0;
	}
	if (nameIndex === whyIndex) {
		return [];
	}
	const pairs: Array<[string, string]> = [];
	for (const row of rows.slice(1)) {
		const name = stripMarkup(row[nameIndex] ?? "");
		const why = stripMarkup(row[whyIndex] ?? "");
		if (name !== "" && why !== "") {
			pairs.push([name, why]);
		}
	}
	return pairs;
}

export function isMarkdownTableLine(line: string): boolean {
	const trimmed = line.trim();
	if (trimmed === "" || !trimmed.includes("|") || trimmed.startsWith("http")) {
		return false;
	}
	if (/^\|?[\s:|-]+\|?$/.test(trimmed)) {
		return true;
	}
	return trimmed.startsWith("|") || trimmed.includes("|");
}

function tableRows(text: string): string[][] {
	const rows: string[][] = [];
	for (const line of text.split(/\r?\n/)) {
		if (!isMarkdownTableLine(line) || isSeparatorRow(line)) {
			continue;
		}
		const parts = tableCells(line);
		if (parts.length > 0) {
			rows.push(parts);
		}
	}
	return rows;
}

function findHeader(header: string[], needles: string[]): number {
	return header.findIndex((cell) => needles.some((needle) => cell.includes(needle)));
}

function tableCells(line: string): string[] {
	let trimmed = line.trim();
	if (trimmed.startsWith("|")) {
		trimmed = trimmed.slice(1);
	}
	if (trimmed.endsWith("|")) {
		trimmed = trimmed.slice(0, -1);
	}
	return trimmed.split("|").map((cell) => cell.trim());
}

function isSeparatorRow(line: string): boolean {
	return tableCells(line).every((cell) => /^:?-+:?$/.test(cell) || cell === "");
}
