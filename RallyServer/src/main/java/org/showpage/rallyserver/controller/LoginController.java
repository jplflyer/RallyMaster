package org.showpage.rallyserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.config.JwtUtil;
import org.showpage.rallyserver.exception.UnauthorizedException;
import org.showpage.rallyserver.service.DtoMapper;
import org.showpage.rallyserver.ui.AuthResponse;
import org.showpage.rallyserver.ui.TokenRequest;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.service.MemberService;
import org.showpage.rallyserver.ui.UiMember;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class LoginController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final MemberService memberService;
    private final ServiceCaller serviceCaller;

    @Operation(
        summary = "Health check endpoint",
        description = "Simple ping endpoint to verify the API is running",
        responses = {
            @ApiResponse(responseCode = "200", description = "API is running")
        }
    )
    @GetMapping("/ping")
    public ResponseEntity<RestResponse<Boolean>> ping() {
        return serviceCaller.call(() -> Boolean.TRUE );
    }

    @Operation(
        summary = "Login with email and password",
        description = "Authenticate using Basic Auth (email:password in Authorization header). Returns access token and refresh token.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
        },
        security = {}
    )
    @PostMapping("/login")
    public ResponseEntity<RestResponse<AuthResponse>> login(
        @Parameter(description = "Basic Auth header with email:password", example = "Basic dXNlckBleGFtcGxlLmNvbTpwYXNzd29yZA==", required = true)
        @RequestHeader("Authorization") String authHeader
    ) {
        return serviceCaller.call( () -> {
            // Extract credentials from Basic Auth header
            String[] credentials = extractCredentials(authHeader);
            String email = credentials[0].trim().toLowerCase();
            String password = credentials[1].trim();

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // Get the member
            Member member = memberService.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(email);
            String refreshToken = jwtUtil.generateRefreshToken(email);

            // Store the refresh token for one-time use validation
            memberService.storeRefreshToken(member, refreshToken);

            return new AuthResponse(accessToken, refreshToken);
        });
    }

    @Operation(
        summary = "Refresh access token",
        description = "Exchange a refresh token for new access and refresh tokens. Refresh tokens are single-use.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
        },
        security = {}
    )
    @PostMapping("/token")
    public ResponseEntity<RestResponse<AuthResponse>> refreshToken(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh token request", required = true)
        @RequestBody TokenRequest tokenRequest
    ) {
        return serviceCaller.call(() -> {
            String refreshToken = tokenRequest.getRefreshToken();

            // Extract email and validate token structure
            String email = jwtUtil.extractEmail(refreshToken);
            if (!jwtUtil.validateToken(refreshToken, email)) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            // Validate and consume the refresh token (can only be used once)
            Member member = memberService.validateAndConsumeRefreshToken(refreshToken, email);

            // Generate new tokens
            String newAccessToken = jwtUtil.generateAccessToken(email);
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Store the new refresh token
            memberService.storeRefreshToken(member, newRefreshToken);

            return new AuthResponse(newAccessToken, newRefreshToken);
        });
    }

    @Operation(
        summary = "Register new member account",
        description = "Create a new member account with email and password",
        responses = {
            @ApiResponse(responseCode = "200", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
        },
        security = {}
    )
    @PostMapping("/register")
    public ResponseEntity<RestResponse<UiMember>> register(
            @Parameter(description = "Email address", example = "rider@example.com", required = true)
            @RequestParam String email,
            @Parameter(description = "Password (will be encrypted)", example = "securePassword123", required = true)
            @RequestParam String password
    ) {
        return serviceCaller.call(() -> DtoMapper.toUiMember(memberService.createMember(email.trim().toLowerCase(), password.trim())));
    }

    private String[] extractCredentials(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }

        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        String[] parts = credentials.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid credentials format");
        }

        return parts;
    }
}