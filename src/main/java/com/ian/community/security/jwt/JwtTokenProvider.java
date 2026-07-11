package com.ian.community.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final MacAlgorithm ALGORITHM =
            MacAlgorithm.HS256;

    private static final String ACCESS_TOKEN_TYPE =
            "access";

    private static final String REFRESH_TOKEN_TYPE =
            "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    private final String issuer;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String encodedSecret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-expiration-seconds}")
            long accessExpirationSeconds,
            @Value("${jwt.refresh-token-expiration-seconds}")
            long refreshExpirationSeconds
    ) {
        this.issuer = issuer;
        this.accessExpirationSeconds =
                accessExpirationSeconds;
        this.refreshExpirationSeconds =
                refreshExpirationSeconds;

        SecretKey secretKey =
                createSecretKey(encodedSecret);

        this.jwtEncoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(ALGORITHM)
                .build();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(ALGORITHM)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer)
        );

        this.jwtDecoder = decoder;
    }

    public String createAccessToken(
            Long userId,
            String email,
            List<String> roles
    ) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(
                        now.plusSeconds(
                                accessExpirationSeconds
                        )
                )
                .id(UUID.randomUUID().toString())
                .claim("token_type", ACCESS_TOKEN_TYPE)
                .claim("email", email)
                .claim("roles", List.copyOf(roles))
                .build();

        return encode(claims);
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(
                        now.plusSeconds(
                                refreshExpirationSeconds
                        )
                )
                .id(UUID.randomUUID().toString())
                .claim("token_type", REFRESH_TOKEN_TYPE)
                .build();

        return encode(claims);
    }

    public Jwt decodeAccessToken(String token) {
        Jwt jwt = decode(token);

        validateTokenType(
                jwt,
                ACCESS_TOKEN_TYPE
        );

        return jwt;
    }

    public Jwt decodeRefreshToken(String token) {
        Jwt jwt = decode(token);

        validateTokenType(
                jwt,
                REFRESH_TOKEN_TYPE
        );

        return jwt;
    }

    public Long getUserId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new JwtException(
                    "사용자 ID 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    public String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public List<String> getRoles(Jwt jwt) {
        List<String> roles =
                jwt.getClaimAsStringList("roles");

        return roles == null
                ? List.of()
                : List.copyOf(roles);
    }

    public String getTokenId(Jwt jwt) {
        String tokenId = jwt.getId();

        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException(
                    "JWT ID가 존재하지 않습니다."
            );
        }

        return tokenId;
    }

    public Instant getExpiresAt(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();

        if (expiresAt == null) {
            throw new JwtException(
                    "JWT 만료 시간이 존재하지 않습니다."
            );
        }

        return expiresAt;
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }

    private Jwt decode(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException(
                    "JWT가 비어 있습니다."
            );
        }

        return jwtDecoder.decode(token);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader
                .with(ALGORITHM)
                .type("JWT")
                .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }

    private void validateTokenType(
            Jwt jwt,
            String expectedType
    ) {
        String tokenType =
                jwt.getClaimAsString("token_type");

        if (!Objects.equals(
                tokenType,
                expectedType
        )) {
            throw new JwtException(
                    "JWT 종류가 올바르지 않습니다."
            );
        }
    }

    private SecretKey createSecretKey(
            String encodedSecret
    ) {
        byte[] decodedKey;

        try {
            decodedKey = Base64
                    .getDecoder()
                    .decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "JWT 비밀키는 Base64 형식이어야 합니다.",
                    exception
            );
        }

        if (decodedKey.length < 32) {
            throw new IllegalArgumentException(
                    "HS256 비밀키는 최소 32바이트 이상이어야 합니다."
            );
        }

        return new SecretKeySpec(
                decodedKey,
                "HmacSHA256"
        );
    }

    public String createInitialRefreshToken(
            Long userId,
            String familyId
    ) {
        return createRefreshToken(
                userId,
                familyId
        );
    }

    public String createRotatedRefreshToken(
            Long userId,
            String familyId
    ) {
        return createRefreshToken(
                userId,
                familyId
        );
    }

    private String createRefreshToken(
            Long userId,
            String familyId
    ) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(
                        issuedAt.plusSeconds(
                                refreshExpirationSeconds
                        )
                )
                .id(UUID.randomUUID().toString())
                .claim(
                        "token_type",
                        REFRESH_TOKEN_TYPE
                )
                .claim("family_id", familyId)
                .build();

        return encode(claims);
    }

    public String getFamilyId(Jwt jwt) {
        String familyId = jwt.getClaimAsString("family_id");

        if (familyId == null || familyId.isBlank()) {
            throw new JwtException(
                    "Refresh Token family ID가 없습니다."
            );
        }

        return familyId;
    }
}