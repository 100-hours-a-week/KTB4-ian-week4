package com.ian.community.security.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "token_family_sessions")
public class TokenFamilySession {

    @Id
    @Column(length = 36)
    private String familyId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 36)
    private String activeAccessTokenId;

    @Column(nullable = false)
    private boolean revoked;

    protected TokenFamilySession() {
    }

    public TokenFamilySession(
            String familyId,
            Long userId,
            String activeAccessTokenId
    ) {
        this.familyId = Objects.requireNonNull(familyId);
        this.userId = Objects.requireNonNull(userId);
        this.activeAccessTokenId = Objects.requireNonNull(activeAccessTokenId);
        this.revoked = false;
    }

    public void rotateAccessToken(String accessTokenId) {
        if (revoked) {
            throw new IllegalStateException("폐기된 Token family입니다.");
        }
        this.activeAccessTokenId = Objects.requireNonNull(accessTokenId);
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean accepts(Long tokenUserId, String accessTokenId) {
        return !revoked
                && userId.equals(tokenUserId)
                && activeAccessTokenId.equals(accessTokenId);
    }

    public String getFamilyId() {
        return familyId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getActiveAccessTokenId() {
        return activeAccessTokenId;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
