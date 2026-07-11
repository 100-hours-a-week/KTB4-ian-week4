package com.ian.community.security.handler;

import tools.jackson.databind.json.JsonMapper;
import com.ian.community.common.ApiResponse;
import com.ian.community.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE =
            "security_exception_error_code";

    private final JsonMapper jsonMapper;

    public CustomAuthenticationEntryPoint(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        ErrorCode errorCode = resolveErrorCode(request);

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

    private ErrorCode resolveErrorCode(
            HttpServletRequest request
    ) {
        Object attribute = request.getAttribute(
                ERROR_CODE_ATTRIBUTE
        );

        if (attribute instanceof ErrorCode errorCode) {
            return errorCode;
        }

        return ErrorCode.UNAUTHORIZED;
    }
}