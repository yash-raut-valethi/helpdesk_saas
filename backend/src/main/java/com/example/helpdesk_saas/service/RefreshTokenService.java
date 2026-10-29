
package com.example.helpdesk_saas.service;

import com.example.helpdesk_saas.dto.TokenRefreshResponseDTO;
import com.example.helpdesk_saas.exception.ExpiredRefreshTokenException;
import com.example.helpdesk_saas.exception.InvalidRefreshTokenException;
import com.example.helpdesk_saas.exception.UserNotFoundException;
import com.example.helpdesk_saas.models.RefreshToken;
import com.example.helpdesk_saas.models.User;
import com.example.helpdesk_saas.repository.RefreshTokenRepository;
import com.example.helpdesk_saas.repository.UserRepository;
import com.example.helpdesk_saas.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtService jwtService) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // Returns the raw token for the HttpOnly cookie.
    // Only its SHA-256 hash is stored in the database.
    @Transactional
    public String createRefreshToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "No user found in current session"
                        )
                );

        String rawToken = generateSecureRefreshToken();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRefreshTokenHash(hashToken(rawToken));
        token.setExpiryDate(
                Instant.now().plusMillis(refreshTokenDurationMs)
        );
        token.setRevoked(false);

        refreshTokenRepository.save(token);

        return rawToken;
    }

    public boolean isTokenExpired(RefreshToken token) {
        return !token.getExpiryDate().isAfter(Instant.now());
    }

    @Transactional
    public TokenRefreshResponseDTO refreshAccessToken(String requestToken) {

        if (requestToken == null || requestToken.isBlank()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token is required."
            );
        }

        String refreshTokenHash = hashToken(requestToken);

        RefreshToken existingToken = refreshTokenRepository
                .findByRefreshTokenHashForUpdate(refreshTokenHash)
                .orElseThrow(() ->
                        new InvalidRefreshTokenException(
                                "Invalid refresh token."
                        )
                );

        if (existingToken.isRevoked()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has already been revoked."
            );
        }

        if (isTokenExpired(existingToken)) {
            throw new ExpiredRefreshTokenException(
                    "Refresh token expired. Please login again."
            );
        }

        User user = existingToken.getUser();

        if (user == null) {
            throw new InvalidRefreshTokenException(
                    "User account is unavailable."
            );
        }

        String newRefreshToken = generateSecureRefreshToken();

        // Revoke the old refresh token.
        existingToken.setRevoked(true);

        RefreshToken replacementToken = new RefreshToken();
        replacementToken.setRefreshTokenHash(
                hashToken(newRefreshToken)
        );
        replacementToken.setUser(user);
        replacementToken.setExpiryDate(
                Instant.now().plusMillis(refreshTokenDurationMs)
        );
        replacementToken.setRevoked(false);

        refreshTokenRepository.save(existingToken);
        refreshTokenRepository.save(replacementToken);

        String newAccessToken = jwtService.generateToken(
                user.getEmail()
        );

        return new TokenRefreshResponseDTO(
                newAccessToken,
                newRefreshToken
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm unavailable.", ex
            );
        }
    }

    private String generateSecureRefreshToken() {

        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
