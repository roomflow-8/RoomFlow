package com.goorm.roomflow.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_002", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_003", "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "리소스를 찾을 수 없습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_005", "서버 오류"),

	// 회의실
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_001", "회의실을 찾을 수 없습니다."),
	ROOM_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ROOM_002", "예약이 가능한 회의실이 아닙니다."),
	ROOM_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_003", "예약 가능 시간을 찾을 수 없습니다."),

	// 예약
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_001", "예약을 찾을 수 없습니다."),
	RESERVATION_CANCELLED(HttpStatus.BAD_REQUEST, "RESERVATION_002", "취소된 예약입니다."),
	RESERVATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "RESERVATION_003", "이미 예약된 시간입니다."),
	DUPLICATE_RESERVATION_REQUEST(HttpStatus.CONFLICT, "RESERVATION_004", "중복된 예약 요청입니다."),
	RESERVATION_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "RESERVATION_005", "이미 확정된 예약입니다."),
	RESERVATION_EXPIRED(HttpStatus.BAD_REQUEST, "RESERVATION_006", "만료된 예약입니다."),
	RESERVATION_NOT_PENDING(HttpStatus.BAD_REQUEST, "RESERVATION_007", "확정 가능한 상태의 예약이 아닙니다."),

	// 비품
	EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EQUIPMENT_001", "비품이 존재하지 않습니다."),
	EQUIPMENT_OUT_OF_STOCK(HttpStatus.CONFLICT, "EQUIPMENT_002", "비품 재고가 부족합니다."),

	// 권한
	NO_PERMISSION(HttpStatus.FORBIDDEN, "AUTH_001", "권한이 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}