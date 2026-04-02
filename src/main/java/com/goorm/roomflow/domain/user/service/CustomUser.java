package com.goorm.roomflow.domain.user.service;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomUser extends User implements OAuth2User {

	private static final long serialVersionUID = 1L; // 직렬화 버전 관리

	private final Long userId;

	@Getter(AccessLevel.NONE)
	private final String name;

	private final String email;

	private final String role;

	@Getter(AccessLevel.NONE)
	private Map<String, Object> attributes;  // OAuth2 속성 저장

	public CustomUser(String email,
					  String password,
					  Collection<? extends GrantedAuthority> authorities,
					  Long userId,    // User Entity의 userId
					  String name,
					  String role) {  // User Entity의 name
		super(email, password, authorities);
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.role = role;
	}

	// OAuth2 로그인용
	public CustomUser(String email,
					  String password,
					  Collection<? extends GrantedAuthority> authorities,
					  Long userId,
					  String name,
					  String role,
					  Map<String, Object> attributes) {
		super(email, password, authorities);
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.role = role;
		this.attributes = attributes;
	}

	// OAuth2User 인터페이스 구현
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * ADMIN 여부 체크
	 */
	public boolean isAdmin() {
		return "ADMIN".equals(role);
	}

	/**
	 * 특정 권한 체크
	 */
	public boolean hasRole(String roleName) {
		return roleName.equals(role);
	}
}