package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.config.JwtUtil;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceCaller {
    private final JwtUtil jwtUtil;

    public interface Lambda<T> {
        T process() throws NotFoundException, ValidationException;
    }

    public interface MemberLambda<T> {
        T process(Member member) throws NotFoundException, ValidationException;
    }

    /**
     * Simple call with no authentication.
     */
    public <T> ResponseEntity<RestResponse<T>> call(Lambda<T> lambda) {
        try {
            T result = lambda.process();
            return ResponseEntity.ok(RestResponse
                    .<T>builder()
                    .success(true)
                    .data(result)
                    .build()
            );
        }
        catch (NotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
        catch (ValidationException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
        catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * This is the most common -- lambda takes the member represented by the JWT.
     */
    public <T> ResponseEntity<RestResponse<T>> call(MemberLambda<T> lambda) {
        try {
            T result = lambda.process(null);
            return ResponseEntity.ok(RestResponse
                    .<T>builder()
                    .success(true)
                    .data(result)
                    .build()
            );
        }
        catch (NotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
        catch (ValidationException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
        catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private <T> ResponseEntity<RestResponse<T>> error(HttpStatus httpStatus, Exception e) {
        return ResponseEntity.status(httpStatus).body(
                RestResponse
                        .<T>builder()
                        .success(false)
                        .message("Exception: " + e.getMessage())
                        .build()
        );
    }

    /**
     * Extract the JWT from the Bearer auth header.
     */
    private static Optional<String> extractToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return Optional.empty();
        }

        // allow case-insensitive "Bearer"
        int space = authorizationHeader.indexOf(' ');
        if (space <= 0) {
            return Optional.empty();
        }

        String scheme = authorizationHeader.substring(0, space);
        String token  = authorizationHeader.substring(space + 1).trim();
        return "bearer".equalsIgnoreCase(scheme) && !token.isEmpty()
                ? Optional.of(token)
                : Optional.empty();
    }
}
