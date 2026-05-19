package com.example.shorturl.exception;

import com.example.shorturl.common.ErrorCode;
import com.example.shorturl.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.StringJoiner;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("biz exception, code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        StringJoiner joiner = new StringJoiner("; ");
        e.getBindingResult().getFieldErrors().forEach(fieldError ->
                joiner.add(fieldError.getField() + ": " + fieldError.getDefaultMessage()));
        log.warn("validation failed: {}", joiner);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), joiner.toString());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("unexpected error", e);
        return Result.error(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
