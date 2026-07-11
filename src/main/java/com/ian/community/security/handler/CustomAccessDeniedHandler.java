package com.ian.community.security.handler;

import tools.jackson.databind.json.JsonMapper;
import com.ian.community.common.ApiResponse;
import com.ian.community.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public CustomAccessDeniedHandler(
            JsonMapper objectMapper
    ) {
        this.jsonMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        response.setStatus(
                errorCode.getStatus().value()
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        ApiResponse<Void> body =
                new ApiResponse<>(
                        errorCode.getMessage(),
                        null
                );

        jsonMapper.writeValue(
                response.getWriter(),
                body
        );
    }
}