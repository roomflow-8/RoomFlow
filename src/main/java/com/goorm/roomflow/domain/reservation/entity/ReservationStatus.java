package com.goorm.roomflow.domain.reservation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
수정:	260321 ES
수정사항:	STATUS ENUM값 - COMPLETED 에서 EXPIRED 로 수정
 */

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
	PENDING("대기"),
	CONFIRMED("확정"),
	CANCELLED("취소"),
	EXPIRED("만료");

	private final String description;
}
////TODO: 차이점 확인
//public enum ReservationStatus {
//	PENDING, CONFIRMED, CANCELLED, EXPIRED
//}

