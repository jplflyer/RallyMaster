package org.showpage.rallyserver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestResponse<T> {
    /**
     * This will get autopouplated by RESTCaller.
     */
    @JsonIgnore
    private int statusCode;

    private boolean success;
    private String message;
    private T data;
}
