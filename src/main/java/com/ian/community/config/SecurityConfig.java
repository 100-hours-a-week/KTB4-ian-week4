package com.ian.community.config;

import com.ian.community.security.handler.CustomAccessDeniedHandler;
import com.ian.community.security.handler.CustomAuthenticationEntryPoint;
import com.ian.community.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.customAuthenticationEntryPoint =
                customAuthenticationEntryPoint;

        this.customAccessDeniedHandler =
                customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .formLogin(
                        AbstractHttpConfigurer::disable
                )

                .httpBasic(
                        AbstractHttpConfigurer::disable
                )

                .cors(
                        Customizer.withDefaults()
                )

                .csrf(csrf ->
                        csrf.spa()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        "/api/csrf",
                                        "/api/users/login",
                                        "/api/users/signup",
                                        "/api/users/refresh",
                                        "/api/users/logout",
                                        "/error",
                                        "/h2-console/**",
                                        "/css/**",
                                        "/js/**",
                                        "/images/**",
                                        "/favicon.ico"
                                ).permitAll()

                                .requestMatchers(
                                        "/api/admin/**"
                                ).hasRole("ADMIN")

                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        customAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        customAccessDeniedHandler
                                )
                )

                .headers(headers ->
                        headers.frameOptions(frame ->
                                frame.sameOrigin()
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}