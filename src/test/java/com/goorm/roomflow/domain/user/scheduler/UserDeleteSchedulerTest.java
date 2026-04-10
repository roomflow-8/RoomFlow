package com.goorm.roomflow.domain.user.scheduler;

import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.entity.UserRole;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/*
* 회원 삭제 스케줄러 테스트
* */
@Transactional
@SpringBootTest
class UserDeleteSchedulerTest {

    @Autowired
    private UserDeleteScheduler userDeleteScheduler;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("삭제 예정 시간이 지난 탈퇴 대기 회원은 영구 삭제된다")
    void deleteExpiredUsers() {
        // 테스트용 더미데이터
        User expiredUser = User.builder()
                .name("expired-user")
                .email("expired@test.com")
                .password("1234")
                .role(UserRole.USER)
                .deletedAt(LocalDateTime.now().minusDays(7))
                .build();

        User pendingUser = User.builder()
                .name("pending-user")
                .email("pending@test.com")
                .password("1234")
                .role(UserRole.USER)
                .deletedAt(LocalDateTime.now().minusDays(6))
                .build();

        User activeUser = User.builder()
                .name("active-user")
                .email("active@test.com")
                .password("1234")
                .role(UserRole.USER)
                .deletedAt(null)
                .build();

        userJpaRepository.save(expiredUser);
        userJpaRepository.save(pendingUser);
        userJpaRepository.save(activeUser);

        em.flush();
        em.clear();

        // 스케줄러 실행
        userDeleteScheduler.deleteExpiredUsers();

        em.flush();
        em.clear();

        // then
        assertThat(userJpaRepository.findById(expiredUser.getUserId())).isEmpty();
        assertThat(userJpaRepository.findById(pendingUser.getUserId())).isPresent();
        assertThat(userJpaRepository.findById(activeUser.getUserId())).isPresent();
    }
}
