package com.ian.community.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user_not_found"),
    USER_ALREADY_DELETED(HttpStatus.CONFLICT, "user_already_deleted"),

    // 회원가입
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "email_already_exists"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "nickname_already_exists"),
    INVALID_SIGNUP_REQUEST(HttpStatus.BAD_REQUEST, "invalid_signup_request"),

    // 로그인
    INVALID_LOGIN_REQUEST(HttpStatus.BAD_REQUEST, "invalid_login_request"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "invalid_password"),

    // 비밀번호 변경
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "current_password_mismatch"),
    NEW_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "new_password_mismatch"),
    PASSWORD_SAME_AS_CURRENT(HttpStatus.CONFLICT, "password_same_as_current"),

    // 게시물 작성/수정
    INVALID_POST_REQUEST(HttpStatus.BAD_REQUEST, "invalid_post_request"),

    // 게시글 & 댓글 수정, 댓글 삭제
    NO_CHANGES_DETECTED(HttpStatus.CONFLICT, "no_changes_detected"),

    // 게시글 삭제
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "post_not_found"),
    POST_ALREADY_DELETED(HttpStatus.CONFLICT, "post_already_deleted"),

    // 댓글
    INVALID_COMMENT_REQUEST(HttpStatus.BAD_REQUEST, "invalid_comment_request"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment_not_found"),
    COMMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "comment_already_deleted"),

    // 인증 인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized"), // 로그인 안 했을 때
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden"), // 로그인은 했지만 권한 없을 때

    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "invalid_access_token"),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "expired_access_token"),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "invalid_refresh_token"),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "expired_refresh_token"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "refresh_token_not_found"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "refresh_token_reused"),
    REFRESH_TOKEN_USER_MISMATCH(HttpStatus.UNAUTHORIZED, "refresh_token_user_mismatch"),
    REFRESH_TOKEN_FAMILY_MISMATCH(HttpStatus.UNAUTHORIZED, "refresh_token_family_mismatch"),

    // 공통
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "image_too_large"),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_image_type"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "too_many_requests"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
