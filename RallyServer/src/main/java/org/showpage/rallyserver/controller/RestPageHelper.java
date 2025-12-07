package org.showpage.rallyserver.controller;

import org.showpage.rallyserver.ui.RestPage.SortOrder;
import org.springframework.data.domain.Page;

/**
 * Helper class for creating RestPage from Spring's Page.
 * The RestPage record itself is in RallyCommon for shared use.
 */
public class RestPageHelper {
    public static <T> org.showpage.rallyserver.ui.RestPage<T> from(Page<T> p) {
        return new org.showpage.rallyserver.ui.RestPage<>(
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
}
