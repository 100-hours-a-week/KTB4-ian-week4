package com.ian.community.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final String code;
    private final String message;
    private final T data;

    public ApiResponse(String message, T data) {
        this(null, message, data);
    }

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
