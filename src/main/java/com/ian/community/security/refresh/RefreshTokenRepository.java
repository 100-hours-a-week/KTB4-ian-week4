package com.ian.community.security.refresh;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(
            String tokenId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rt
            from RefreshToken rt
            where rt.tokenId = :tokenId
            """)
    Optional<RefreshToken> findByTokenIdForUpdate(
            @Param("tokenId") String tokenId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.familyId = :familyId
              and rt.revoked = false
            """)
    int revokeAllByFamilyId(
            @Param("familyId") String familyId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.userId = :userId
              and rt.revoked = false
            """)
    int revokeAllByUserId(
            @Param("userId") Long userId
    );
}