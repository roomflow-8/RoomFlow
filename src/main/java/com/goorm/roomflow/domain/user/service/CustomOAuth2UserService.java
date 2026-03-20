package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.entity.SocialAccount;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import com.goorm.roomflow.domain.user.oauth.OAuth2UserInfo;
import com.goorm.roomflow.domain.user.repository.SocialAccountRepository;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserJpaRepository userJpaRepository;
	private final SocialAccountRepository socialAccountRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		String userNameAttributeName = userRequest.getClientRegistration()
				.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

		OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

		// 1. 소셜 계정으로 이미 연동된 유저 찾기
		User user = socialAccountRepository
				.findByProviderAndProviderUserId(userInfo.getProvider(), userInfo.getProviderId())
				.map(SocialAccount::getUser)
				.orElse(null);

		if (user == null) {
			// 2. 이메일로 기존 유저 찾기 (자동 연동)
			String email = userInfo.getEmail();
			if (email != null) {
				user = userJpaRepository.findByEmail(email);
			}

			if (user == null) {
				// 3. 신규 유저 생성
				user = User.builder()
						.name(userInfo.getName())
						.email(email != null ? email : userInfo.getProvider() + "_" + userInfo.getProviderId())
						.password(UUID.randomUUID().toString()) // 소셜 로그인 유저는 임의 비밀번호
						.role(UserRole.USER)
						.build();
				userJpaRepository.save(user);
				log.info("소셜 로그인 신규 유저 생성: provider={}, email={}", userInfo.getProvider(), email);
			}

			// 소셜 계정 연동
			SocialAccount socialAccount = SocialAccount.builder()
					.user(user)
					.provider(userInfo.getProvider())
					.providerUserId(userInfo.getProviderId())
					.createdAt(LocalDateTime.now())
					.build();
			socialAccountRepository.save(socialAccount);
			log.info("소셜 계정 연동 완료: provider={}, userId={}", userInfo.getProvider(), user.getUserId());
		}

		return new DefaultOAuth2User(
				Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
				oAuth2User.getAttributes(),
				userNameAttributeName
		);
	}
}
