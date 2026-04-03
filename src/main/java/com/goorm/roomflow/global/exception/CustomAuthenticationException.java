package com.goorm.roomflow.global.exception;

import com.goorm.roomflow.global.code.ErrorCode;
import org.springframework.security.core.AuthenticationException;

public class CustomAuthenticationException extends AuthenticationException {

    private final ErrorCode errorCode;
    private final String email;

    public CustomAuthenticationException(ErrorCode errorCode, String email) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.email = email;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getEmail() {
        return email;
    }
}