package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.global.exception.CustomAuthenticationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FormLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        CustomAuthenticationException customException = null;

        if (exception instanceof CustomAuthenticationException ex) {
            customException = ex;
        } else if (exception.getCause() instanceof CustomAuthenticationException ex) {
            customException = ex;
        }

        if (customException != null) {
            String email = customException.getEmail();
            String errorCode = customException.getErrorCode().name();

            response.sendRedirect(
                    "/users/login?error=" + errorCode
                            + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
            );
            return;
        }

        response.sendRedirect("/users/login?error=LOGIN_FAILED");
    }
}
