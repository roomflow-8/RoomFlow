package com.goorm.roomflow.domain.payment.dto.request;

public record PaymentFailReq(
		String code,
		String message,
		String orderId
) {
}
