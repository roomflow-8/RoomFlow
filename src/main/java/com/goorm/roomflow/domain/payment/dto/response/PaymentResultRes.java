package com.goorm.roomflow.domain.payment.dto.response;

import com.goorm.roomflow.domain.payment.entity.Payment;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentResultRes(
		Long paymentId,
		String orderId,
		String orderName,
		BigDecimal totalAmount,
		String method,
		String approvedAt,
		String receiptUrl
) {
	public static PaymentResultRes from(Payment payment) {
		return PaymentResultRes.builder()
				.paymentId(payment.getPaymentId())
				.orderId(payment.getOrderId())
				.orderName(payment.getOrderName())
				.totalAmount(payment.getTotalAmount())
				.method(payment.getMethod())
				.approvedAt(payment.getApprovedAt() != null
						? payment.getApprovedAt().toString() : null)
				.receiptUrl(payment.getReceiptUrl())
				.build();
	}
}