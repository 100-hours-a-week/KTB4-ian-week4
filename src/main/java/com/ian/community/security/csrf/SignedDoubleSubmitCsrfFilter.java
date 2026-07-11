package com.ian.community.security.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class SignedDoubleSubmitCsrfFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS =
            Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SignedDoubleSubmitCsrfService csrfService;
    private final CsrfBindingResolver bindingResolver;

    public SignedDoubleSubmitCsrfFilter(
            SignedDoubleSubmitCsrfService csrfService,
            CsrfBindingResolver bindingResolver
    ) {
        this.csrfService = csrfService;
        this.bindingResolver = bindingResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SAFE_METHODS.contains(request.getMethod())
                || path.startsWith("/h2-console")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String cookieToken = bindingResolver.cookieValue(
                request,
                CsrfCookieProvider.TOKEN_COOKIE
        );
        String headerToken = request.getHeader(
                CsrfCookieProvider.TOKEN_HEADER
        );
        String binding = bindingResolver.resolve(request).orElse(null);

        boolean sameToken = cookieToken != null
                && headerToken != null
                && MessageDigest.isEqual(
                        cookieToken.getBytes(StandardCharsets.US_ASCII),
                        headerToken.getBytes(StandardCharsets.US_ASCII)
                );

        if (!sameToken || !csrfService.verify(binding, headerToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"invalid_csrf_token\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
