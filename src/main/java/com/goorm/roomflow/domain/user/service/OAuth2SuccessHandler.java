package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UserJpaRepository userJpaRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {

		log.info("소셜 로그인 성공: {}", authentication.getName());

		// OAuth2User에서 이메일 추출 후 세션에 loginUser 저장
		if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
			String email = (String) oAuth2User.getAttributes().get("email");

			// 카카오는 email이 kakao_account 안에 있음
			if (email == null) {
				Object kakaoAccount = oAuth2User.getAttributes().get("kakao_account");
				if (kakaoAccount instanceof java.util.Map<?, ?> map) {
					email = (String) map.get("email");
				}
			}

			// 네이버는 response 안에 있음
			if (email == null) {
				Object naverResponse = oAuth2User.getAttributes().get("response");
				if (naverResponse instanceof java.util.Map<?, ?> map) {
					email = (String) map.get("email");
				}
			}

			if (email != null) {
				User user = userJpaRepository.findByEmail(email);
				if (user != null) {
					UserTO loginUser = new UserTO();
					loginUser.setUserId(user.getUserId());
					loginUser.setName(user.getName());
					loginUser.setEmail(user.getEmail());
					loginUser.setRole(user.getRole().name());

					request.getSession().setAttribute("loginUser", loginUser);
					log.info("소셜 로그인 - 세션 저장: {}", email);
				}
			}
		}

		setDefaultTargetUrl("/rooms");
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
