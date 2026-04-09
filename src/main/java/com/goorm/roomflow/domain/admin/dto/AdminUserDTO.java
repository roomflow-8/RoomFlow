package com.goorm.roomflow.domain.admin.dto;

import java.time.LocalDateTime;

/*
 * 관리자 회원 목록 조회용 DTO
 * - lastLoginAt, visitCount, totalPayment 는 user_history 테이블 구현 후 추가 예정
 * TODO: user_history 테이블 생성 후 lastLoginAt / visitCount / totalPayment 필드 추가
 */
public record AdminUserDTO(
        Long userId,
        String name,
        String email,
        String role,           // USER / ADMIN
        LocalDateTime createdAt,   // 가입일
        LocalDateTime deletedAt,   // null 이면 정상 회원, 값 있으면 탈퇴 대기 중
        long reservationCount,     // 전체 예약 건수
        long cancelCount           // 취소 예약 건수
) {
    // 탈퇴 대기 회원 여부
    public boolean isPendingDelete() {
        return deletedAt != null;
    }

    // 탈퇴 후 남은 일수 (7일 기준)
    public long remainingDays() {
        if (deletedAt == null) return -1;
        long elapsed = java.time.temporal.ChronoUnit.DAYS.between(deletedAt, LocalDateTime.now());
        return Math.max(0, 7 - elapsed);
    }
}
