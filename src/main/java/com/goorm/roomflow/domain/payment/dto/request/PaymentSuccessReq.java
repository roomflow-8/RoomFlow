package com.goorm.roomflow.domain.payment.dto.request;

public record PaymentSuccessReq(
		String orderId,
		String paymentKey,
		Long amount
) {
}
