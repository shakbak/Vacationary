package com.vacationary.messaging.domain.render;

public final class ReplyTextStripper {

	private ReplyTextStripper() {
	}

	public static String stripTablesAndDumps(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		StringBuilder kept = new StringBuilder();
		for (String line : text.split("\\r?\\n", -1)) {
			if (isMarkdownTableLine(line) || isStayDumpLine(line)) {
				continue;
			}
			if (kept.length() > 0) {
				kept.append('\n');
			}
			kept.append(line);
		}
		return kept.toString().replaceAll("\\n{3,}", "\n\n").trim();
	}

	private static boolean isMarkdownTableLine(String line) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || !trimmed.contains("|") || trimmed.startsWith("http")) {
			return false;
		}
		return trimmed.startsWith("|") || trimmed.indexOf('|') > 0;
	}

	private static boolean isStayDumpLine(String line) {
		String trimmed = line.trim();
		if (!trimmed.matches("^\\d+\\.\\s+.*")) {
			return false;
		}
		return trimmed.contains("Why:") || trimmed.contains("/ night") || trimmed.contains("Rating");
	}
}
