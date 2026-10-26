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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Value("${jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo, UserRepository userRepo, JwtService jwtService) {
        this.refreshTokenRepository = repo;
        this.userRepository = userRepo;
        this.jwtService = jwtService;
    }

    public RefreshToken createRefreshToken(String email) {
        var token = new RefreshToken();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if(optionalUser.isEmpty()){
            throw new UserNotFoundException("No user found in current session");
        }
        token.setUser(optionalUser.get());
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        token.setRefreshTokenHash(UUID.randomUUID().toString());
        return refreshTokenRepository.save(token);
    }

    public boolean isTokenExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }

    @Transactional
    public TokenRefreshResponseDTO refreshAccessToken(String requestToken) {

        // 1. Validate the incoming token
        if (requestToken == null || requestToken.isBlank()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token is required."
            );
        }

        // 2. Hash the supplied token
        String refreshTokenHash = hashToken(requestToken);

        // 3. Find and lock the token record
        RefreshToken existingToken = refreshTokenRepository
                .findByRefreshTokenHashForUpdate(refreshTokenHash)
                .orElseThrow(() ->
                        new InvalidRefreshTokenException(
                                "Invalid refresh token."
                        )
                );

        // 4. Reject revoked or previously used tokens
        if (existingToken.isRevoked()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has already been revoked."
            );
        }

        // 5. Validate expiration
        if (!existingToken.getExpiryDate().isAfter(Instant.now())) {
            System.out.println("Refresh Token expired");
            throw new ExpiredRefreshTokenException(
                    "Refresh token expired. Please login again."
            );
        }

        // 6. Validate associated user
        User user = existingToken.getUser();

        if (user == null) {
            throw new InvalidRefreshTokenException(
                    "User account is unavailable."
            );
        }

        // 7. Generate a new secure refresh token
        String newRefreshToken = generateSecureRefreshToken();

        // 8. Revoke the old token
        existingToken.setRevoked(true);

        // 9. Create a new refresh-token record
        RefreshToken replacementToken = new RefreshToken();

        replacementToken.setRefreshTokenHash(hashToken(newRefreshToken));
        replacementToken.setUser(user);
        replacementToken.setExpiryDate(
                Instant.now().plus(Duration.ofDays(7))
        );
        replacementToken.setRevoked(false);

        // 10. Persist changes atomically
        refreshTokenRepository.save(existingToken);
        refreshTokenRepository.save(replacementToken);

        // 11. Generate a new access JWT
        System.out.println("Generating New /Refreshing access token");
        String newAccessToken = jwtService.generateToken(
                user.getEmail()
        );
        System.out.println("Generated new access token");

        // 12. Return both credentials
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