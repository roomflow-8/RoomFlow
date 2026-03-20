package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.mapper.UserMapper;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.domain.user.entity.SocialAccount;
import com.goorm.roomflow.domain.user.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    private final SocialAccountRepository socialAccountRepository;

    public UserTO login(String email, String password) {

        // 1. 이메일로 DB 조회
        User user = userJpaRepository.findByEmail(email);

        // 2. 이메일 없으면 null 반환
        if (user == null) {
            return null;
        }

        // 3. 탈퇴한 회원 확인
        if (user.isDeleted()) {
            return null;
        }

        // 4. 비밀번호 비교
        if (!user.getPassword().equals(password)) {
            return null;
        }

        // 5. 로그인 성공 → UserTO 반환
        return userMapper.toUserTO(user);
    }

    public UserTO findByEmail(String email) {
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            return null;
        }
        return userMapper.toUserTO(user);
    }

    public List<SocialAccount> findSocialAccountsByUserId(Long userId) {
        return socialAccountRepository.findAll().stream()
                .filter(sa -> sa.getUser().getUserId().equals(userId))
                .toList();
    }
}
