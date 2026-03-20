package com.goorm.roomflow.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "User API", description = "유저 REST API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserRestController {

	@Operation(summary = "현재 로그인 유저 정보 조회", description = "OAuth2 로그인된 유저의 정보를 반환합니다.")
	@GetMapping("/me")
	public ResponseEntity<Map<String, Object>> getCurrentUser(
			@AuthenticationPrincipal OAuth2User oAuth2User) {

		if (oAuth2User == null) {
			return ResponseEntity.status(401).build();
		}

		return ResponseEntity.ok(oAuth2User.getAttributes());
	}
}
