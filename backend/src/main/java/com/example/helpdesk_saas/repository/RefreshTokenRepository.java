package com.example.helpdesk_saas.repository;

import com.example.helpdesk_saas.models.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
//    Optional<RefreshToken> findByRefreshTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT rt
    FROM RefreshToken rt
    JOIN FETCH rt.user
    WHERE rt.refreshTokenHash = :refreshTokenHash
""")
    Optional<RefreshToken> findByRefreshTokenHashForUpdate(
            @Param("refreshTokenHash") String refreshTokenHash
    );
}
