package com.goorm.roomflow.domain.user.oauth;

import java.util.Map;

public interface OAuth2UserInfo {
	String getProvider();
	String getProviderId();
	String getEmail();
	String getName();

	static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
		return switch (registrationId) {
			case "google" -> new GoogleOAuth2UserInfo(attributes);
			case "kakao" -> new KakaoOAuth2UserInfo(attributes);
			case "naver" -> new NaverOAuth2UserInfo(attributes);
			default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인: " + registrationId);
		};
	}
}
