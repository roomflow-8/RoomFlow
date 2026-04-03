package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.dto.UserDto;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final OAuth2AuthorizedClientService authorizedClientService;
	private final SocialAccountService socialAccountService;

	private final UserJpaRepository userJpaRepository;
/* 원본
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
	*/

	//CustomUser 반영본
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {

		if (authentication.getPrincipal() instanceof CustomUser customUser) {
			log.info("소셜 로그인 성공: userId={}", customUser.getUserId());

			// UserDTO 생성 (CustomUser에서 바로 추출)
			UserDto loginUser = UserDto.from(customUser);

			request.getSession().setAttribute("loginUser", loginUser);
			log.info("소셜 로그인 - 세션 저장 완료: userId={}", customUser.getUserId());

			// refresh Token 저장
			if(authentication instanceof OAuth2AuthenticationToken oauth2Token) {
				String provider = oauth2Token.getAuthorizedClientRegistrationId();

				OAuth2AuthorizedClient authorizedClient =
						authorizedClientService.loadAuthorizedClient(
								provider,
								oauth2Token.getName()
						);

				if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
					String refreshToken = authorizedClient.getRefreshToken().getTokenValue();

					socialAccountService.updateRefreshToken( customUser.getUserId(), provider, refreshToken);

					log.info("refreshToken 저장 완료: userId={}, provider={}",
							customUser.getUserId(), provider);
				} else {
					log.info("refreshToken 없음: userId={}, provider={}",
							customUser.getUserId(), provider);
				}
			}

		} else {
			log.warn("예상치 못한 Principal 타입: {}", authentication.getPrincipal().getClass());
		}

		setDefaultTargetUrl("/rooms");
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
