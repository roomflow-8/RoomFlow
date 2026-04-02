package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserJpaRepository userJpaRepository;
/*
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userJpaRepository.findByEmail(email);

		if (user == null || user.isDeleted()) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
		}

		return new org.springframework.security.core.userdetails.User(
				user.getEmail(),
				user.getPassword(),
				Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
		);
	}*/


	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userJpaRepository.findByEmail(email);

		if (user == null || user.isDeleted()) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}

		return new CustomUser(
				user.getEmail(),
				user.getPassword(),
				Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
				user.getUserId(),     // userId 포함
				user.getName(),      // name 포함
				user.getRole().name()  // "USER" or "ADMIN"
		);
	}
}
