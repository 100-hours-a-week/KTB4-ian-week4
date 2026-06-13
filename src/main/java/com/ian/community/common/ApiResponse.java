package com.ian.community.common;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final String message;
    private final T data;

    public ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }
}
