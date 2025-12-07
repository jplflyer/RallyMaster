package org.showpage.rallyserver.ui;

import java.util.List;

/**
 * REST API pagination wrapper.
 * Simplified version of Spring's Page for use in REST responses.
 */
public record RestPage<T>(
        List<T> content,
        int page,               // 0-based
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<SortOrder> sort    // optional
) {
    public record SortOrder(String property, String direction) {}
}
