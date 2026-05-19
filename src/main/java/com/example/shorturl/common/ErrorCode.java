package com.example.shorturl.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "bad request"),
    UNAUTHORIZED(40100, "unauthorized"),
    FORBIDDEN(40300, "forbidden"),
    NOT_FOUND(40400, "not found"),
    CONFLICT(40900, "conflict"),
    SHORT_CODE_EXHAUSTED(40901, "short code exhausted"),
    INTERNAL_ERROR(50000, "internal error"),
    SERVICE_UNAVAILABLE(50300, "service unavailable"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
