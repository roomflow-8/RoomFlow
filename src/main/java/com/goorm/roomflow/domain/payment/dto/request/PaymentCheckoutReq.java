package com.goorm.roomflow.domain.payment.dto.request;

import java.math.BigDecimal;

public record PaymentCheckoutReq(
		Long amount,       // 결제 금액
		String orderId,    // 고유 주문 번호
		String orderName,   // 주문명 (예: "A회의실 14:00~16:00 예약, 비품 2건") BigDecimal roomAmount,
		Long reservationId,
        BigDecimal roomAmount,
		BigDecimal equipmentAmount
) {
	public static PaymentCheckoutReq of(Long amount, String orderId, String orderName) {
		return new PaymentCheckoutReq(amount, orderId, orderName, null, null, null);
	}
	public static PaymentCheckoutReq of(Long amount, String orderId, String orderName,
										Long reservationId, BigDecimal roomAmount, BigDecimal equipmentAmount) {
		return new PaymentCheckoutReq(amount, orderId, orderName, reservationId, roomAmount, equipmentAmount);
	}
}
