package com.fintech.authservice.service;

import com.fintech.authservice.domain.RefreshToken;
import com.fintech.authservice.domain.User;
import com.fintech.authservice.dto.request.LoginRequest;
import com.fintech.authservice.dto.request.RegisterRequest;
import com.fintech.authservice.dto.response.AuthResponse;
import com.fintech.authservice.exception.AuthException;
import com.fintech.authservice.exception.UserAlreadyExistsException;
import com.fintech.authservice.repository.RefreshTokenRepository;
import com.fintech.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Service for user authentication and registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }

        // Create user
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .roles(Set.of(User.Role.USER))
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

        // Generate tokens
        return generateAuthResponse(savedUser);
    }

    /**
     * Authenticate user and return tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            log.warn("Login failed for {}: {}", request.email(), e.getMessage());
            throw new AuthException("Invalid email or password");
        }

        // Get user
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("User not found"));

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        // Validate
        if (!refreshToken.isValid()) {
            throw new AuthException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        // Check user is still active
        if (!user.isEnabled()) {
            throw new AuthException("User account is disabled");
        }

        // Revoke old refresh token (rotation for security)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Logout user by revoking all refresh tokens.
     */
    @Transactional
    public void logout(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElse(null);

        if (refreshToken != null) {
            // Revoke all tokens for this user
            refreshTokenRepository.revokeAllTokensForUser(refreshToken.getUser());
            log.info("User logged out: {}", refreshToken.getUser().getEmail());
        }
    }

    /**
     * Validate an access token and return user info.
     */
    public User validateAccessToken(String token) {
        if (!jwtService.validateToken(token)) {
            throw new AuthException("Invalid token");
        }

        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));
    }

    /**
     * Generate auth response with access and refresh tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user);

        // Save refresh token to database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenString,
                jwtService.getAccessTokenExpiration() / 1000,  // Convert to seconds
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    /**
     * Find user by email (for internal service lookups).
     */
    public java.util.Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

