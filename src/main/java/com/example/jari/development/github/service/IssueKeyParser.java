package com.example.jari.development.github.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Jira-style issue keys (e.g. APP-1, KAN-42) from free text.
 */
@Component
public class IssueKeyParser {

    private static final Pattern KEY_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");

    public List<String> extractKeys(String... texts) {
        Set<String> keys = new LinkedHashSet<>();
        if (texts == null) {
            return List.of();
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher m = KEY_PATTERN.matcher(text);
            while (m.find()) {
                keys.add(m.group(1).toUpperCase());
            }
        }
        return List.copyOf(keys);
    }
}
