package com.vacationary.backend.domain.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StayWhyParser {

	private static final Pattern NUMBERED_WHY = Pattern.compile(
			"^\\s*\\d+\\.\\s+(?:\\*\\*(.+?)\\*\\*|(.+?))\\s+[–—-].*?(?:\\*[Ww]hy:\\*|\\bWhy:)\\s*(.+)$");
	private static final Pattern TRAILING_MARKDOWN_LINK = Pattern.compile(
			"\\s*[–—-]?\\s*\\[[^\\]]*]\\([^)]*\\)\\s*$");

	private StayWhyParser() {
	}

	static String whyForStay(String replyText, String stayName) {
		String wanted = normalizeName(stayName);
		if (wanted.isEmpty() || replyText == null || replyText.isBlank()) {
			return null;
		}
		String best = null;
		int bestScore = 0;
		for (String[] pair : parsePairs(replyText)) {
			int score = nameMatchScore(wanted, normalizeName(pair[0]));
			if (score > bestScore) {
				bestScore = score;
				best = pair[1];
			}
		}
		return best;
	}

	private static List<String[]> parsePairs(String text) {
		List<String[]> pairs = new ArrayList<>();
		pairs.addAll(parseWhyColumn(text));
		pairs.addAll(parseNumberedWhy(text));
		return pairs;
	}

	private static List<String[]> parseWhyColumn(String text) {
		List<List<String>> rows = new ArrayList<>();
		for (String line : text.split("\\r?\\n")) {
			if (!isMarkdownTableLine(line) || isSeparatorRow(line)) {
				continue;
			}
			List<String> parts = tableCells(line);
			if (!parts.isEmpty()) {
				rows.add(parts);
			}
		}
		if (rows.size() < 2) {
			return List.of();
		}
		List<String> header = rows.get(0).stream().map(cell -> cell.toLowerCase(Locale.ROOT)).toList();
		int nameIndex = findHeader(header, "name", "stay", "hotel", "property", "listing");
		int whyIndex = findHeader(header, "why", "stand", "highlight", "fit");
		if (whyIndex < 0) {
			return List.of();
		}
		if (nameIndex < 0) {
			nameIndex = 0;
		}
		if (nameIndex == whyIndex) {
			return List.of();
		}
		List<String[]> pairs = new ArrayList<>();
		for (List<String> row : rows.subList(1, rows.size())) {
			String name = stripMarkup(cell(row, nameIndex));
			String why = stripMarkup(cell(row, whyIndex));
			if (!name.isEmpty() && !why.isEmpty()) {
				pairs.add(new String[] { name, why });
			}
		}
		return pairs;
	}

	private static List<String[]> parseNumberedWhy(String text) {
		List<String[]> pairs = new ArrayList<>();
		for (String line : text.split("\\r?\\n")) {
			Matcher matcher = NUMBERED_WHY.matcher(line);
			if (!matcher.find()) {
				continue;
			}
			String name = stripMarkup(firstNonNull(matcher.group(1), matcher.group(2)));
			String why = stripMarkup(matcher.group(3));
			why = TRAILING_MARKDOWN_LINK.matcher(why).replaceAll("").trim();
			if (!name.isEmpty() && !why.isEmpty()) {
				pairs.add(new String[] { name, why });
			}
		}
		return pairs;
	}

	private static int findHeader(List<String> header, String... needles) {
		for (int i = 0; i < header.size(); i++) {
			String cell = header.get(i);
			for (String needle : needles) {
				if (cell.contains(needle)) {
					return i;
				}
			}
		}
		return -1;
	}

	private static String cell(List<String> row, int index) {
		if (index < 0 || index >= row.size()) {
			return "";
		}
		return row.get(index);
	}

	private static List<String> tableCells(String line) {
		String trimmed = line.trim();
		if (trimmed.startsWith("|")) {
			trimmed = trimmed.substring(1);
		}
		if (trimmed.endsWith("|")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		String[] raw = trimmed.split("\\|", -1);
		List<String> cells = new ArrayList<>(raw.length);
		for (String cell : raw) {
			cells.add(cell.trim());
		}
		return cells;
	}

	private static boolean isSeparatorRow(String line) {
		for (String cell : tableCells(line)) {
			if (!cell.isEmpty() && !cell.matches("^:?-+:?$")) {
				return false;
			}
		}
		return true;
	}

	private static boolean isMarkdownTableLine(String line) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || !trimmed.contains("|") || trimmed.startsWith("http")) {
			return false;
		}
		return trimmed.startsWith("|") || trimmed.indexOf('|') > 0;
	}

	private static String normalizeName(String name) {
		return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}

	private static int nameMatchScore(String optionName, String tableName) {
		if (tableName.isEmpty() || optionName.isEmpty()) {
			return 0;
		}
		if (optionName.equals(tableName)) {
			return 3;
		}
		if (optionName.contains(tableName) || tableName.contains(optionName)) {
			return 2;
		}
		return 0;
	}

	private static String stripMarkup(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("**", "").replace("*", "").trim();
	}

	private static String firstNonNull(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second == null ? "" : second;
	}
}
