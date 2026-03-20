package com.goorm.roomflow.domain.user.oauth;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

	private final Map<String, Object> attributes;

	@SuppressWarnings("unchecked")
	public NaverOAuth2UserInfo(Map<String, Object> attributes) {
		// 네이버는 response 안에 실제 유저 정보가 들어있음
		this.attributes = (Map<String, Object>) attributes.get("response");
	}

	@Override
	public String getProvider() {
		return "naver";
	}

	@Override
	public String getProviderId() {
		return (String) attributes.get("id");
	}

	@Override
	public String getEmail() {
		return (String) attributes.get("email");
	}

	@Override
	public String getName() {
		return (String) attributes.get("name");
	}
}
