package com.ian.community.security.csrf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SignedDoubleSubmitCsrfService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int RANDOM_BYTES = 32;

    private final byte[] secret;
    private final SecureRandom secureRandom = new SecureRandom();

    public SignedDoubleSubmitCsrfService(
            @Value("${csrf.secret}") String encodedSecret
    ) {
        try {
            this.secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "CSRF 비밀키는 Base64 형식이어야 합니다.",
                    exception
            );
        }

        if (secret.length < 32) {
            throw new IllegalArgumentException(
                    "CSRF HMAC 비밀키는 최소 32바이트 이상이어야 합니다."
            );
        }
    }

    public CsrfTokenPair issueAnonymous() {
        return issue(randomValue());
    }

    public CsrfTokenPair issue(String sessionBinding) {
        if (sessionBinding == null || sessionBinding.isBlank()) {
            throw new IllegalArgumentException(
                    "CSRF 세션 바인딩 값이 비어 있습니다."
            );
        }

        String randomValue = randomValue();
        String signature = sign(sessionBinding, randomValue);

        return new CsrfTokenPair(
                sessionBinding,
                signature + "." + randomValue
        );
    }

    public boolean verify(
            String sessionBinding,
            String signedToken
    ) {
        if (sessionBinding == null
                || sessionBinding.isBlank()
                || signedToken == null
                || signedToken.isBlank()) {
            return false;
        }

        int separator = signedToken.indexOf('.');

        if (separator <= 0 || separator == signedToken.length() - 1) {
            return false;
        }

        String providedSignature = signedToken.substring(0, separator);
        String randomValue = signedToken.substring(separator + 1);
        String expectedSignature = sign(sessionBinding, randomValue);

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                providedSignature.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String sign(
            String sessionBinding,
            String randomValue
    ) {
        String message = sessionBinding.length()
                + "!"
                + sessionBinding
                + "!"
                + randomValue.length()
                + "!"
                + randomValue;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            mac.doFinal(
                                    message.getBytes(StandardCharsets.UTF_8)
                            )
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "CSRF 토큰 서명 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private String randomValue() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
