package com.ian.community.security.session;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenFamilySessionRepository
        extends JpaRepository<TokenFamilySession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from TokenFamilySession session where session.familyId = :familyId")
    Optional<TokenFamilySession> findByFamilyIdForUpdate(
            @Param("familyId") String familyId
    );

    @Modifying(clearAutomatically = true)
    @Query("update TokenFamilySession session set session.revoked = true where session.familyId = :familyId")
    int revokeByFamilyId(@Param("familyId") String familyId);

    @Modifying(clearAutomatically = true)
    @Query("update TokenFamilySession session set session.revoked = true where session.userId = :userId")
    int revokeAllByUserId(@Param("userId") Long userId);
}
