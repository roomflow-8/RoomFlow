package com.goorm.roomflow.domain.user.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
					   Authentication authentication) throws IOException, ServletException {

		log.info("소셜 로그인 성공: {}", authentication.getName());

		// 로그인 성공 후 리다이렉트 (필요에 따라 변경)
		setDefaultTargetUrl("/rooms");
		super.handle(request, response, authentication);
	}
}
