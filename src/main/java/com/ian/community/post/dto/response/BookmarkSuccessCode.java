package com.ian.community.post.dto.response;

import com.ian.community.common.SuccessCode;
import org.springframework.http.HttpStatus;

public enum BookmarkSuccessCode implements SuccessCode {
    BOOKMARK_CREATED("북마크를 저장했습니다."),
    BOOKMARK_ALREADY_SAVED("이미 저장된 북마크입니다."),
    BOOKMARK_DELETED("북마크를 삭제했습니다."),
    BOOKMARK_LIST_FOUND("북마크 목록을 조회했습니다."),
    NO_MORE_BOOKMARKS("더 이상 조회할 북마크가 없습니다.");

    private final String message;

    BookmarkSuccessCode(String message) {
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
