package com.goorm.roomflow.domain.user.scheduler;

import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteScheduler {

    private final UserJpaRepository userRepository;
    private final UserService userService;

    /*
    * 7일 후 유저 삭제 로직 구현
    * - 탈퇴 후 7일 보관 -> 다음날 00:30 삭제
    * */
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Seoul")
    public void deleteExpiredUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        // 삭제 대상 유저 정보 조회
        List<User> users = userRepository.findAllByDeletedAtBefore(threshold);

        if(users.isEmpty()) {
            log.info("[UserDelete] 영구 삭제 대상 회원이 없습니다. now={}, threshold={}", LocalDateTime.now(), threshold);
            return;
        }

        log.info("[UserDelete] 영구 삭제 대상 회원 수 = {}, now={}, threshold={}", users.size(), LocalDateTime.now(), threshold);

        int successCount = 0;
        int failedCount = 0;

        for(User user : users) {
            try{
                userService.hardDeleteUser(user);
                successCount++;
            } catch(Exception e) {
                failedCount++;
                log.error("[UserDelete] 회원 영구 삭제 실패 - userId={}, email={}", user.getUserId(), user.getEmail(), e);
            }
        }

        log.info("[UserDelete] 회원 영구 삭제 스케줄러 종료 - successCount={}, failCount={}", successCount, failedCount);
    }

}
