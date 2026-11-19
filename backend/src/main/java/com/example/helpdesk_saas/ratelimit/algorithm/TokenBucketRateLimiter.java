package com.example.helpdesk_saas.ratelimit.algorithm;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;

public class TokenBucketRateLimiter {

    private final Bucket bucket;

    public TokenBucketRateLimiter() {

        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillIntervally(1, Duration.ofSeconds(1))
                .build();

        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean allowRequest() {
        return bucket.tryConsume(1);
    }
}