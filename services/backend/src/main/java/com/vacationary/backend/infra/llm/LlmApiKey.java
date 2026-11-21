package com.vacationary.backend.infra.llm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LlmApiKey {

	private LlmApiKey() {
	}

	public static String resolve(String configured) {
		return resolve(configured, Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize());
	}

	static String resolve(String configured, Path startDir) {
		if (hasText(configured)) {
			return configured.trim();
		}
		String env = System.getenv("VACATIONARY_LLM_API_KEY");
		if (hasText(env)) {
			return env.trim();
		}
		Path dir = startDir;
		for (int i = 0; i < 4 && dir != null; i++) {
			String fromFile = readFirstLine(dir.resolve("sec.txt"));
			if (hasText(fromFile)) {
				return fromFile;
			}
			dir = dir.getParent();
		}
		return "";
	}

	private static String readFirstLine(Path file) {
		if (!Files.isRegularFile(file)) {
			return "";
		}
		try {
			for (String line : Files.readAllLines(file)) {
				if (line == null) {
					continue;
				}
				String trimmed = line.trim();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					return trimmed;
				}
			}
			return "";
		}
		catch (IOException ignored) {
			return "";
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
