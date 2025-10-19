package org.showpage.rallyserver.controller;

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
public class LoginController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final MemberService memberService;
    private final ServiceCaller serviceCaller;

    @GetMapping("/ping")
    public ResponseEntity<RestResponse<Boolean>> ping() {
        return serviceCaller.call(() -> Boolean.TRUE );
    }

    @PostMapping("/login")
    public ResponseEntity<RestResponse<AuthResponse>> login(@RequestHeader("Authorization") String authHeader) {
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

    @PostMapping("/token")
    public ResponseEntity<RestResponse<AuthResponse>> refreshToken(@RequestBody TokenRequest tokenRequest) {
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

    @PostMapping("/register")
    public ResponseEntity<RestResponse<UiMember>> register(
            @RequestParam String email,
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