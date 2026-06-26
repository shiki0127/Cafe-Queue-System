package com.cafequeue.common.core;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class IdGenerator {
    private static final Map<String, String> READABLE_PREFIXES = Map.of(
            "ord", "A",
            "que", "B"
    );
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private IdGenerator() {
    }

    public static String prefixed(String prefix) {
        int sequence = SEQUENCE.updateAndGet(current -> current >= 9999 ? 1 : current + 1);
        String normalized = prefix.toLowerCase(Locale.ROOT);
        String label = READABLE_PREFIXES.getOrDefault(normalized, normalized.substring(0, 1).toUpperCase(Locale.ROOT));
        return label + String.format("%03d", sequence);
    }
}
