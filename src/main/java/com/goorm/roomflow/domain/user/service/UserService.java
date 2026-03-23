package com.goorm.roomflow.domain.user.service;

import com.goorm.roomflow.domain.user.dto.SignupRequestDTO;
import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.SocialAccount;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import com.goorm.roomflow.domain.user.mapper.UserMapper;
import com.goorm.roomflow.domain.user.repository.SocialAccountRepository;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.domain.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;


    public void signup(SignupRequestDTO request) {

        // 이름 유효성 검사
        String name = request.getName().trim();
        if (!UserValidator.isValidName(name)) {
            throw new IllegalArgumentException("이름은 한글 2~10자 또는 영문 2~20자로 입력해주세요.");
        }

        // 이메일 형식 검사
        if (!UserValidator.isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 이메일 중복 확인
        if (userJpaRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 유효성 검사
        if (!UserValidator.isValidPassword(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }

        // 비밀번호 확인 일치 여부
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        User user = User.builder()
                .name(name)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        userJpaRepository.save(user);
    }

    public void updateName(String email, String newName) {
        String trimmed = newName.trim();
        if (!UserValidator.isValidName(trimmed)) {
            throw new IllegalArgumentException("이름은 한글 2~10자 또는 영문 2~20자로 입력해주세요.");
        }
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        user.updateName(trimmed);
        userJpaRepository.save(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (!UserValidator.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
        userJpaRepository.save(user);
    }

    public void deleteAccount(String email) {
        User user = userJpaRepository.findByEmail(email);
        if (user == null || user.isDeleted()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        user.delete();
        userJpaRepository.save(user);
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
