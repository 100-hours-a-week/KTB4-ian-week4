package com.ian.community.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/csrf")
public class CsrfController {

    private final SignedDoubleSubmitCsrfService csrfService;
    private final CsrfBindingResolver bindingResolver;
    private final CsrfCookieProvider cookieProvider;

    public CsrfController(
            SignedDoubleSubmitCsrfService csrfService,
            CsrfBindingResolver bindingResolver,
            CsrfCookieProvider cookieProvider
    ) {
        this.csrfService = csrfService;
        this.bindingResolver = bindingResolver;
        this.cookieProvider = cookieProvider;
    }

    @GetMapping
    public ResponseEntity<Void> issue(HttpServletRequest request) {
        String existingBinding = bindingResolver.resolve(request).orElse(null);
        boolean needsSessionBinding = existingBinding == null;
        String rawSessionBinding = needsSessionBinding
                ? UUID.randomUUID().toString()
                : null;
        String binding = needsSessionBinding
                ? "session:" + rawSessionBinding
                : existingBinding;

        CsrfTokenPair pair = csrfService.issue(binding);
        ResponseEntity.BodyBuilder response = ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieProvider.createTokenCookie(pair.token()).toString()
                );

        if (needsSessionBinding) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    cookieProvider
                            .createPreAuthBindingCookie(rawSessionBinding)
                            .toString()
            );
        }

        return response.build();
    }
}
