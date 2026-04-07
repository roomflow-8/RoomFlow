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

	// 사용자
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
	USER_NOT_DELETED(HttpStatus.BAD_REQUEST, "USER_002", "탈퇴 처리된 회원만 영구 삭제할 수 있습니다."),
	USER_DELETED(HttpStatus.UNAUTHORIZED, "USER_003", "탈퇴 처리된 계정입니다."),

	UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH_001", "지원하지 않는 소셜 제공자입니다."),
	SOCIAL_ACCESS_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH_002","소셜 access token이 존재하지 않습니다."),
	SOCIAL_REFRESH_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH_003","소셜 refresh token이 존재하지 않습니다."),
	SOCIAL_UNLINK_FAILED(HttpStatus.BAD_GATEWAY, "OAUTH_004", "소셜 계정 연동 해제에 실패했습니다."),

	// 회의실
	ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_001", "회의실을 찾을 수 없습니다."),
	ROOM_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ROOM_002", "예약이 가능한 회의실이 아닙니다."),
	ROOM_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_003", "예약 가능 시간을 찾을 수 없습니다."),
	POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_001", "정책 정보를 찾을 수 없습니다."),

	// 예약
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_001", "예약을 찾을 수 없습니다."),
	RESERVATION_CANCELLED(HttpStatus.BAD_REQUEST, "RESERVATION_002", "이미 취소된 예약입니다."),
	RESERVATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "RESERVATION_003", "이미 예약된 시간입니다."),
	DUPLICATE_RESERVATION_REQUEST(HttpStatus.CONFLICT, "RESERVATION_004", "중복된 예약 요청입니다."),
	RESERVATION_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "RESERVATION_005", "이미 확정된 예약입니다."),
	RESERVATION_EXPIRED(HttpStatus.BAD_REQUEST, "RESERVATION_006", "만료된 예약입니다."),
	INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "RESERVATION_007", "요청을 처리할 수 없는 예약 상태입니다."),
	RESERVATION_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "RESERVATION_008", "확정되지 않은 예약입니다."),
	
	RESERVATION_EQUIPMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "RESERVATION_009", "비품 예약을 조회할 수 없습니다."),
	INVALID_RESERVATION_EQUIPMENT(HttpStatus.BAD_REQUEST, "RESERVATION_010", "유효하지 않은 예약ID 입니다."),
	RESERVATION_EQUIPMENT_CANCELLED(HttpStatus.BAD_REQUEST, "RESERVATION_011", "이미 취소된 비품 예약입니다."),
  	ALREADY_PROCESSING_RESERVATION(HttpStatus.CONFLICT, "RESERVATION_012", "이미 해당 시간대 예약이 진행 중입니다."),
	RESERVATION_FORBIDDEN(HttpStatus.FORBIDDEN, "RESERVATION_013", "해당 예약에 대한 권한이 없습니다."),
	RESERVATION_EQUIPMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "RESERVATION_014", "비품 예약을 취소할 수 없습니다."),
	RESERVATION_EQUIPMENT_FAILED(HttpStatus.BAD_REQUEST, "RESERVATION_015", "비품 예약 중 에러가 발생했습니다."),

	// 비품
	EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EQUIPMENT_001", "비품이 존재하지 않습니다."),
	EQUIPMENT_OUT_OF_STOCK(HttpStatus.CONFLICT, "EQUIPMENT_002", "비품 재고가 부족합니다."),
	EQUIPMENT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "EQUIPMENT_003", "사용할 수 없는 비품입니다."),
	EQUIPMENT_ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "EQUIPMENT_004", "유효하지 않은 수량입니다."),

	// 권한
	NO_PERMISSION(HttpStatus.FORBIDDEN, "AUTH_001", "권한이 없습니다."),

	// 결제
	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001", "결제 정보를 찾을 수 없습니다."),
	PAYMENT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PAYMENT_002", "이미 결제 요청이 존재합니다."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_003", "결제 금액이 일치하지 않습니다."),
	PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_004", "결제를 진행할 수 없는 상태입니다."),
	PAYMENT_CONFIRM_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_005", "결제 승인에 실패했습니다."),
	PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_006", "결제 취소에 실패했습니다."),
	PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PAYMENT_007", "이미 결제가 완료된 예약입니다."),

	// 토스페이먼츠 에러
	PAY_PROCESS_CANCELED(HttpStatus.BAD_REQUEST, "PAYMENT_008", "구매자에 의해 결제가 취소되었습니다."),
	PAY_PROCESS_ABORTED(HttpStatus.BAD_REQUEST, "PAYMENT_009", "결제 인증에 실패했습니다."),
	REJECT_CARD_COMPANY(HttpStatus.BAD_REQUEST, "PAYMENT_010", "카드사에서 결제를 거부했습니다. 해당 카드사에 문의해주세요."),
	UNAUTHORIZED_KEY(HttpStatus.UNAUTHORIZED, "PAYMENT_011", "API키를 다시 확인해주세요."),
	NOT_FOUND_PAYMENT_SESSION(HttpStatus.NOT_FOUND, "PAYMENT_012", "결제 시간이 만료되어 결제 진행 데이터가 존재하지 않습니다."),
	FORBIDDEN_REQUEST(HttpStatus.FORBIDDEN, "PAYMENT_013", "API키 또는 주문번호를 다시 확인해주세요.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}