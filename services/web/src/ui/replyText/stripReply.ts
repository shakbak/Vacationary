import { isMarkdownTableLine } from "./markdownTable";
import { isStayDumpLine } from "./numberedStay";

export function stripMarkdownTables(text: string): string {
	const withoutDumpLines = text
		.split(/\r?\n/)
		.filter((line) => !isMarkdownTableLine(line) && !isStayDumpLine(line))
		.join("\n");
	return withoutDumpLines.replace(/\n{3,}/g, "\n\n").trim();
}
