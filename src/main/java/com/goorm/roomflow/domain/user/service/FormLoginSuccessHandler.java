package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.dto.UserDto;
import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
//@RequiredArgsConstructor
public class FormLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	//private final UserJpaRepository userJpaRepository;
/*
//원본
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {

		String email = authentication.getName();
		User user = userJpaRepository.findByEmail(email);

		if (user != null) {
			UserTO loginUser = new UserTO();
			loginUser.setUserId(user.getUserId());
			loginUser.setName(user.getName());
			loginUser.setEmail(user.getEmail());
			loginUser.setRole(user.getRole().name());

			request.getSession().setAttribute("loginUser", loginUser);
			log.info("일반 로그인 성공 - 세션 저장: {}", email);
		}

		setDefaultTargetUrl("/rooms");
		super.onAuthenticationSuccess(request, response, authentication);
	}
	*/

	//CustomUser 적용
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {

		// CustomUser로 간단하게 처리
		if (authentication.getPrincipal() instanceof CustomUser customUser) {
			log.info("일반 로그인 성공: userId={}", customUser.getUserId());

			// Record 생성자 또는 정적 팩토리 메서드 사용
			UserDto loginUser = UserDto.from(customUser);

			request.getSession().setAttribute("loginUser", loginUser);
			log.info("일반 로그인 - 세션 저장 완료: userId={}", customUser.getUserId());
		} else {
			log.warn("예상치 못한 Principal 타입: {}", authentication.getPrincipal().getClass());
		}
		setDefaultTargetUrl("/rooms");
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
