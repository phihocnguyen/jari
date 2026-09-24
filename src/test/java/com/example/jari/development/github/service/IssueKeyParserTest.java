package com.example.jari.development.github.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueKeyParserTest {

    private final IssueKeyParser parser = new IssueKeyParser();

    @Test
    void extractsKeysFromTitleAndBranch() {
        List<String> keys = parser.extractKeys("fix: APP-1 login crash", "feature/APP-1-login", "KAN-42");
        assertEquals(List.of("APP-1", "KAN-42"), keys);
    }

    @Test
    void ignoresInvalidPatterns() {
        List<String> keys = parser.extractKeys("fixed a-1 bug", "no key here", null, "");
        assertTrue(keys.isEmpty());
    }

    @Test
    void dedupesCaseInsensitive() {
        List<String> keys = parser.extractKeys("app-1 and APP-1");
        assertEquals(List.of("APP-1"), keys);
    }
}
