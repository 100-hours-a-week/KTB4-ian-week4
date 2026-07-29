package com.ian.community.post.dto.response;

import com.ian.community.common.SuccessCode;
import org.springframework.http.HttpStatus;

public enum PostSuccessCode implements SuccessCode {
    POST_LIST_FOUND("피드 목록을 조회했습니다."),
    NO_MORE_POSTS("더 이상 조회할 피드가 없습니다.");

    private final String message;

    PostSuccessCode(String message) {
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.OK;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
