package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.showpage.rallyserver.interfaces.HasId;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.exception.NotFoundException;
import org.showpage.rallyserver.exception.ValidationException;
import org.showpage.rallyserver.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCaller {
    private final MemberRepository memberRepository;

    public interface Lambda<T> {
        T process() throws NotFoundException, ValidationException, DataIntegrityViolationException;
    }

    public interface MemberLambda<T> {
        T process(Member member) throws NotFoundException, ValidationException, DataIntegrityViolationException;
    }

    /**
     * Simple call with no authentication.
     */
    @Transactional
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
        catch (NotFoundException e)   {
            log.warn("NotFoundException", e);
            return error(HttpStatus.NOT_FOUND, e);
        }
        catch (ValidationException e) {
            log.warn("ValidationException", e);
            return error(HttpStatus.BAD_REQUEST, e);
        }
        catch (DataIntegrityViolationException e) {
            log.warn("DataIntegrityViolationException", e);
            return error(HttpStatus.CONFLICT, e);
        }
        catch (Exception e)           {
            log.warn("Exception", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * This is the most common -- lambda takes the member represented by the JWT.
     */
    @Transactional
    public <T> ResponseEntity<RestResponse<T>> call(MemberLambda<T> lambda) {
        try {
            T result = lambda.process(getCurrentMember());

            return ResponseEntity.ok(RestResponse
                    .<T>builder()
                    .success(true)
                    .data(result)
                    .build()
            );
        }
        catch (NotFoundException e)     {
            log.warn("NotFoundException", e);
            return error(HttpStatus.NOT_FOUND, e);
        }
        catch (ValidationException e)   {
            log.warn("ValidationException", e);
            return error(HttpStatus.BAD_REQUEST, e);
        }
        catch (UnauthorizedException e) {
            log.warn("UnauthorizedException", e);
            return error(HttpStatus.UNAUTHORIZED, e);
        }
        catch (DataIntegrityViolationException e) {
            return error(HttpStatus.CONFLICT, e);
        }
        catch (Exception e)             {
            log.warn("Exception", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Use this for a create.
     */
    @Transactional
    public <T extends HasId<T>> ResponseEntity<RestResponse<T>> call(String prefix, MemberLambda<T> lambda) {
        try {
            T result = lambda.process(getCurrentMember());

            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path(prefix.endsWith("/") ? prefix + "{id}" : prefix + "/{id}")
                    .buildAndExpand(result.getId())
                    .toUri();

            return ResponseEntity.created(location)
                    .body(RestResponse
                        .<T>builder()
                        .success(true)
                        .data(result)
                        .build()
                );
        }
        catch (NotFoundException e)     {
            log.warn("NotFoundException", e);
            return error(HttpStatus.NOT_FOUND, e);
        }
        catch (ValidationException e)   {
            log.warn("ValidationException", e);
            return error(HttpStatus.BAD_REQUEST, e);
        }
        catch (UnauthorizedException e) {
            log.warn("UnauthorizedException", e);
            return error(HttpStatus.UNAUTHORIZED, e);
        }
        catch (DataIntegrityViolationException e) {
            return error(HttpStatus.CONFLICT, e);
        }
        catch (Exception e)             {
            log.warn("Exception", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }


    //======================================================================
    // Helpers.
    //======================================================================

    /**
     * Gets the current user.
     */
    private Member getCurrentMember() throws UnauthorizedException {
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx != null) {
            Authentication auth = ctx.getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String name = auth.getName();
                if (name != null && !name.isBlank()) {
                    return memberRepository.findByEmail(name).orElseThrow(() -> new UnauthorizedException("Unauthorized"));
                }
            }
        }

        throw new UnauthorizedException("Unauthorized");
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

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String msg) { super(msg); }
    }

}
