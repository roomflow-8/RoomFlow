package com.goorm.roomflow.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentCancelStatus {

	PENDING("취소 대기"),
	DONE("취소 완료"),
	FAILED("취소 실패");

	private final String description;
}