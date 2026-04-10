package com.goorm.roomflow.domain.user.dto;

import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.service.CustomUser;

public record UserDto(
	Long userId,
	String name,
	String email,
	String role
) {
		public static UserDto from(CustomUser customUser) {
			return new UserDto(
					customUser.getUserId(),
					customUser.getName(),
					customUser.getEmail(),
					customUser.getAuthorities().iterator().next().getAuthority()
			);
		}

	public static UserDto from(User user) {
		return new UserDto(
				user.getUserId(),
				user.getName(),
				user.getEmail(),
				user.getRole().name()
		);
	}
}
