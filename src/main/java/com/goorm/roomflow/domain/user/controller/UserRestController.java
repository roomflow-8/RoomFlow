package com.goorm.roomflow.domain.user.controller;

import com.goorm.roomflow.domain.user.service.CustomUser;
import com.goorm.roomflow.domain.user.service.UserService;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "User API", description = "유저 REST API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserRestController {

	private final UserService userService;

	@Operation(summary = "현재 로그인 유저 정보 조회", description = "OAuth2 로그인된 유저의 정보를 반환합니다.")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
			@AuthenticationPrincipal CustomUser currentUser) {

		if (currentUser == null) {
			return ResponseEntity.status(401).build();
		}

		Map<String, Object> response = new HashMap<>();
		response.put("userId", currentUser.getUserId());
		response.put("email", currentUser.getEmail());
		response.put("name", currentUser.getName());
		response.put("authorities", currentUser.getAuthorities());


		// OAuth2 로그인 여부
		boolean isOAuth2 = currentUser.getAttributes() != null;
		response.put("isOAuth2Login", isOAuth2);

		// OAuth2 로그인인 경우 추가 정보
		if (isOAuth2) {
			response.put("oAuth2Attributes", currentUser.getAttributes());
		}

		log.info("현재 사용자 조회: userId={}, email={}, isOAuth2={}",
				currentUser.getUserId(), currentUser.getEmail(), isOAuth2);

		return ApiResponse.success(
				SuccessCode.OK, response);
	}

	@Operation(summary = "회원 영구 삭제", description = "소셜로그인 연결을 끊고, 개인 정보를 영구 삭제합니다.")
	@DeleteMapping("/delete/{userId}")
	public ResponseEntity<ApiResponse<Void>> deleteUser(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long userId
	) {
		userService.hardDeleteUserById(userId);

		return ApiResponse.success(SuccessCode.OK);
	}
}
