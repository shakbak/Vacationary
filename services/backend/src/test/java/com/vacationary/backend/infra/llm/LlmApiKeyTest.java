package com.vacationary.backend.infra.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LlmApiKeyTest {

	@Test
	void prefersConfiguredValue(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("sec.txt"), "from-file");
		assertEquals("from-property", LlmApiKey.resolve("from-property", dir));
	}

	@Test
	void readsSecTxtWhenConfiguredIsBlank(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("sec.txt"), "# comment\n  file-key  \n");
		assertEquals("file-key", LlmApiKey.resolve("  ", dir));
	}

	@Test
	void walksParentsForSecTxt(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("sec.txt"), "parent-key");
		Path nested = Files.createDirectories(dir.resolve("services").resolve("backend"));
		assertEquals("parent-key", LlmApiKey.resolve("", nested));
	}

	@Test
	void blankWhenMissing(@TempDir Path dir) {
		assertEquals("", LlmApiKey.resolve("", dir));
	}
}
