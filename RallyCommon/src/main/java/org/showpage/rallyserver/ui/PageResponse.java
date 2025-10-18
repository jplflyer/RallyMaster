package org.showpage.rallyserver.ui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Simple DTO for deserializing Spring Data Page responses on the client side.
 * This avoids Jackson issues with the abstract Page interface.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    @JsonCreator
    public PageResponse(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") int number,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("first") boolean first,
            @JsonProperty("last") boolean last,
            @JsonProperty("empty") boolean empty
    ) {
        this.content = content;
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }
}
