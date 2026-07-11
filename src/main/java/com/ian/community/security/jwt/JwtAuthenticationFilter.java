package com.ian.community.security.jwt;

import com.ian.community.common.exception.ErrorCode;
import com.ian.community.security.handler.CustomAuthenticationEntryPoint;
import com.ian.community.security.principal.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        return path.equals("/api/users/login")
                || path.equals("/api/users/signup")
                || path.equals("/api/users/refresh")
                || path.equals("/api/users/logout")
                || path.startsWith("/h2-console")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = resolveAccessToken(request);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication currentAuthentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (currentAuthentication != null
                && currentAuthentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Jwt jwt = jwtTokenProvider.decodeAccessToken(
                    accessToken
            );

            AuthenticatedUser authenticatedUser =
                    createAuthenticatedUser(jwt);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            authenticatedUser.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();

            request.setAttribute(
                    CustomAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE,
                    ErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser createAuthenticatedUser(
            Jwt jwt
    ) {
        Long userId = jwtTokenProvider.getUserId(jwt);
        String email = jwtTokenProvider.getEmail(jwt);

        List<SimpleGrantedAuthority> authorities =
                jwtTokenProvider.getRoles(jwt)
                        .stream()
                        .map(this::normalizeRole)
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        return new AuthenticatedUser(
                userId,
                email,
                authorities
        );
    }

    private String normalizeRole(
            String role
    ) {
        if (role == null || role.isBlank()) {
            throw new JwtException(
                    "JWT 권한이 비어 있습니다."
            );
        }

        String normalizedRole =
                role.trim().toUpperCase();

        if (normalizedRole.startsWith("ROLE_")) {
            return normalizedRole;
        }

        return "ROLE_" + normalizedRole;
    }

    private String resolveAccessToken(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie ->
                        JwtCookieProvider
                                .ACCESS_TOKEN_COOKIE
                                .equals(cookie.getName())
                )
                .map(Cookie::getValue)
                .filter(value ->
                        value != null
                                && !value.isBlank()
                )
                .findFirst()
                .orElse(null);
    }
}