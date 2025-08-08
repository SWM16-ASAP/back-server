package com.linglevel.api.common.handler;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ExceptionResponse> handleCommonException(CommonException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoHandlerFoundException(NoResourceFoundException e) {
        CommonException commonException = new CommonException(CommonErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleGenericException(Exception e) {
        CommonException commonException = new CommonException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(commonException.getStatus())
                .body(new ExceptionResponse(commonException));
    }
}