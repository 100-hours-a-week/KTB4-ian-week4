package com.ian.community.security.refresh;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refreshTokenId;

    @Column(
            nullable = false,
            unique = true,
            length = 36
    )
    private String tokenId;

    @Column(
            nullable = false,
            length = 36
    )
    private String familyId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(length = 36)
    private String replacedByTokenId;

    protected RefreshToken() {
    }

    public RefreshToken(
            String tokenId,
            String familyId,
            Long userId,
            Instant expiresAt
    ) {
        this.tokenId = Objects.requireNonNull(
                tokenId,
                "Refresh Token ID는 null일 수 없습니다."
        );

        this.familyId = Objects.requireNonNull(
                familyId,
                "Refresh Token family ID는 null일 수 없습니다."
        );

        this.userId = Objects.requireNonNull(
                userId,
                "Refresh Token 사용자 ID는 null일 수 없습니다."
        );

        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "Refresh Token 만료 시간은 null일 수 없습니다."
        );

        this.revoked = false;
        this.replacedByTokenId = null;
    }

    public void rotateTo(
            String newTokenId
    ) {
        if (revoked) {
            throw new IllegalStateException(
                    "이미 폐기된 Refresh Token입니다."
            );
        }

        this.revoked = true;
        this.replacedByTokenId = Objects.requireNonNull(
                newTokenId,
                "새 Refresh Token ID는 null일 수 없습니다."
        );
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(
                Instant.now()
        );
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Long getRefreshTokenId() {
        return refreshTokenId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getReplacedByTokenId() {
        return replacedByTokenId;
    }
}