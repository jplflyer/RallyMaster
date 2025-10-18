package org.showpage.rallyserver.controller;

import lombok.RequiredArgsConstructor;
import org.showpage.rallyserver.RestResponse;
import org.showpage.rallyserver.config.JwtUtil;
import org.showpage.rallyserver.ui.AuthResponse;
import org.showpage.rallyserver.ui.TokenRequest;
import org.showpage.rallyserver.entity.Member;
import org.showpage.rallyserver.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
    public ResponseEntity<?> login(@RequestHeader("Authorization") String authHeader) {
        return serviceCaller.call( () -> {
            // Extract credentials from Basic Auth header
            String[] credentials = extractCredentials(authHeader);
            String email = credentials[0];
            String password = credentials[1];

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(email);
            String refreshToken = jwtUtil.generateRefreshToken(email);

            return new AuthResponse(accessToken, refreshToken);
        });
    }

    @PostMapping("/token")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRequest tokenRequest) {
        try {
            String refreshToken = tokenRequest.getRefreshToken();
            String email = jwtUtil.extractEmail(refreshToken);

            // Validate refresh token
            Optional<Member> member = memberService.findByEmail(email);
            if (member.isPresent() && jwtUtil.validateToken(refreshToken, email)) {
                // Generate new tokens
                String newAccessToken = jwtUtil.generateAccessToken(email);
                String newRefreshToken = jwtUtil.generateRefreshToken(email);

                return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RestResponse<Member>> register(
            @RequestParam String email,
            @RequestParam String password
    ) {
        return serviceCaller.call(() -> memberService.createMember(email, password));
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