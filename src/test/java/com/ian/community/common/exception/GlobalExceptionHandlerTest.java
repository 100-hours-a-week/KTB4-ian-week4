package com.ian.community.common.exception;

import com.ian.community.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    @DisplayName("CustomException은 Error Code의 HTTP Status와 Code를 보존한다")
    void customExceptionContract() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleCustomException(
                        new CustomException(ErrorCode.POST_NOT_FOUND)
                );

        assertThat(response.getStatusCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("POST_NOT_FOUND");
        assertThat(response.getBody().getMessage())
                .isEqualTo("게시글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("예상하지 못한 예외는 내부 Message를 노출하지 않는다")
    void hideUnexpectedExceptionDetails() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnexpectedException(
                        new IllegalStateException(
                                "database-password=secret"
                        )
                );

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage())
                .doesNotContain("database-password")
                .doesNotContain("secret");
    }
}
