package com.example.helpdesk_saas.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    public class RefreshToken extends Basemodel{ ;

    @Column(
            name = "refresh_token_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String refreshTokenHash;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    }

