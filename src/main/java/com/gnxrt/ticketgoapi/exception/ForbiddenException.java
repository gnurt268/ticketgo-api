package com.gnxrt.ticketgoapi.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        super("Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN);
    }
}