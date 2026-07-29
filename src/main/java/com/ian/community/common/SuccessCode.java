package com.ian.community.common;

import org.springframework.http.HttpStatus;

public interface SuccessCode {
    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
