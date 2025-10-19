package org.showpage.rallyserver.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

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
    public static <T> RestPage<T> from(Page<T> p) {
        return new RestPage<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast(),
                p.getSort().stream()
                        .map(o -> new SortOrder(o.getProperty(), o.isAscending() ? "ASC" : "DESC"))
                        .toList()
        );
    }

    public record SortOrder(String property, String direction) {}
}
