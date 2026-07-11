package com.ian.community.security.token;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.security.jwt.JwtTokenProvider;
import com.ian.community.security.refresh.RefreshToken;
import com.ian.community.security.refresh.RefreshTokenRepository;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public TokenService(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public TokenPair issueInitialTokens(
            User user
    ) {
        validateActiveUser(user);

        String familyId = UUID.randomUUID().toString();

        String accessToken =
                jwtTokenProvider.createAccessToken(
                        user.getUserId(),
                        user.getEmail(),
                        List.of("USER")
                );

        String refreshToken =
                jwtTokenProvider.createInitialRefreshToken(
                        user.getUserId(),
                        familyId
                );

        Jwt refreshJwt =
                decodeRefreshTokenOrThrow(
                        refreshToken
                );

        RefreshToken refreshTokenEntity =
                new RefreshToken(
                        jwtTokenProvider.getTokenId(
                                refreshJwt
                        ),
                        familyId,
                        user.getUserId(),
                        jwtTokenProvider.getExpiresAt(
                                refreshJwt
                        )
                );

        refreshTokenRepository.save(
                refreshTokenEntity
        );

        return new TokenPair(
                accessToken,
                refreshToken
        );
    }

    public TokenPair rotate(
            String rawRefreshToken
    ) {
        Jwt oldRefreshJwt =
                decodeRefreshTokenOrThrow(
                        rawRefreshToken
                );

        String oldTokenId =
                jwtTokenProvider.getTokenId(
                        oldRefreshJwt
                );

        String familyId =
                jwtTokenProvider.getFamilyId(
                        oldRefreshJwt
                );

        Long userId =
                jwtTokenProvider.getUserId(
                        oldRefreshJwt
                );

        RefreshToken savedToken =
                refreshTokenRepository
                        .findByTokenIdForUpdate(
                                oldTokenId
                        )
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.REFRESH_TOKEN_NOT_FOUND
                                )
                        );

        validateRefreshTokenOwner(
                savedToken,
                userId
        );

        validateRefreshTokenFamily(
                savedToken,
                familyId
        );

        validateRefreshTokenReusable(
                savedToken
        );

        validateRefreshTokenNotExpired(
                savedToken
        );

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        validateActiveUser(user);

        String newRefreshToken =
                jwtTokenProvider.createRotatedRefreshToken(
                        userId,
                        familyId
                );

        Jwt newRefreshJwt =
                decodeRefreshTokenOrThrow(
                        newRefreshToken
                );

        String newTokenId =
                jwtTokenProvider.getTokenId(
                        newRefreshJwt
                );

        savedToken.rotateTo(
                newTokenId
        );

        RefreshToken newRefreshTokenEntity =
                new RefreshToken(
                        newTokenId,
                        familyId,
                        userId,
                        jwtTokenProvider.getExpiresAt(
                                newRefreshJwt
                        )
                );

        refreshTokenRepository.save(
                newRefreshTokenEntity
        );

        String newAccessToken =
                jwtTokenProvider.createAccessToken(
                        user.getUserId(),
                        user.getEmail(),
                        List.of("USER")
                );

        return new TokenPair(
                newAccessToken,
                newRefreshToken
        );
    }

    public void logout(
            String rawRefreshToken
    ) {
        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()) {
            return;
        }

        try {
            Jwt jwt =
                    jwtTokenProvider.decodeRefreshToken(
                            rawRefreshToken
                    );

            String tokenId =
                    jwtTokenProvider.getTokenId(jwt);

            refreshTokenRepository
                    .findByTokenIdForUpdate(tokenId)
                    .ifPresent(
                            RefreshToken::revoke
                    );

        } catch (JwtException | IllegalArgumentException exception) {
            // 로그아웃에서는 토큰이 이미 만료되었거나 잘못되어도
            // 브라우저 쿠키 삭제는 계속 진행해야 한다.
        }
    }

    public void logoutAll(
            Long userId
    ) {
        refreshTokenRepository.revokeAllByUserId(
                userId
        );
    }

    private Jwt decodeRefreshTokenOrThrow(
            String rawRefreshToken
    ) {
        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()) {
            throw new CustomException(
                    ErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        try {
            return jwtTokenProvider.decodeRefreshToken(
                    rawRefreshToken
            );
        } catch (JwtException | IllegalArgumentException exception) {
            if (isExpiredTokenException(exception)) {
                throw new CustomException(
                        ErrorCode.EXPIRED_REFRESH_TOKEN
                );
            }

            throw new CustomException(
                    ErrorCode.INVALID_REFRESH_TOKEN
            );
        }
    }

    private boolean isExpiredTokenException(
            Exception exception
    ) {
        String message = exception.getMessage();

        if (message == null) {
            return false;
        }

        return message.toLowerCase()
                .contains("expired");
    }

    private void validateRefreshTokenOwner(
            RefreshToken savedToken,
            Long userId
    ) {
        if (!savedToken.getUserId().equals(userId)) {
            refreshTokenRepository.revokeAllByFamilyId(
                    savedToken.getFamilyId()
            );

            throw new CustomException(
                    ErrorCode.REFRESH_TOKEN_USER_MISMATCH
            );
        }
    }

    private void validateRefreshTokenFamily(
            RefreshToken savedToken,
            String familyId
    ) {
        if (!savedToken.getFamilyId().equals(familyId)) {
            refreshTokenRepository.revokeAllByFamilyId(
                    savedToken.getFamilyId()
            );

            throw new CustomException(
                    ErrorCode.REFRESH_TOKEN_FAMILY_MISMATCH
            );
        }
    }

    private void validateRefreshTokenReusable(
            RefreshToken savedToken
    ) {
        if (savedToken.isRevoked()) {
            refreshTokenRepository.revokeAllByFamilyId(
                    savedToken.getFamilyId()
            );

            throw new CustomException(
                    ErrorCode.REFRESH_TOKEN_REUSED
            );
        }
    }

    private void validateRefreshTokenNotExpired(
            RefreshToken savedToken
    ) {
        if (savedToken.isExpired()) {
            savedToken.revoke();

            throw new CustomException(
                    ErrorCode.EXPIRED_REFRESH_TOKEN
            );
        }
    }

    private void validateActiveUser(
            User user
    ) {
        if (user.isUserDeleted()) {
            throw new CustomException(
                    ErrorCode.USER_ALREADY_DELETED
            );
        }
    }
}