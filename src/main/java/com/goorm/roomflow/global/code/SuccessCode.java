package com.goorm.roomflow.global.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode {

    // 공통
    OK(HttpStatus.OK, "COMMON_001", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON_002", "생성되었습니다."),

    // 인증
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH_001", "로그인에 성공했습니다."),
    SIGNUP_SUCCESS(HttpStatus.CREATED, "AUTH_002", "회원가입이 완료되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_003", "로그아웃되었습니다."),

    // 회의실 조회
    ROOM_SUCCESS(HttpStatus.OK, "ROOM_001", "회의실 조회 성공"),
    ROOMSLOT_CREATED(HttpStatus.CREATED, "ROOM_002", "시간 생성 성공"),

    // 예약
    RESERVATION_SUCCESS(HttpStatus.OK, "RESERVATION_001", "예약 조회 성공"),
    RESERVATION_CREATED(HttpStatus.CREATED, "RESERVATION_002", "예약 생성 성공"),
    RESERVATION_CANCELLED(HttpStatus.OK, "RESERVATION_003", "예약 취소 성공"),

    // 비품
    EQUIPMENT_SUCCESS(HttpStatus.OK, "EQUIPMENT_001", "비품 조회 성공"),
    EQUIPMENT_ADDED(HttpStatus.CREATED, "EQUIPMENT_002", "비품 추가 성공"),
    EQUIPMENT_CANCELLED(HttpStatus.OK, "EQUIPMENT_003", "비품 취소 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}