package com.goorm.roomflow.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentRes(
		String paymentKey,
		String orderId,
		String orderName,
		String status,
		String method,
		String requestedAt,
		String approvedAt,
		Long totalAmount,
		Long balanceAmount,
		Card card,
		Receipt receipt,
		Failure failure,
		List<Cancel> cancels
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Card(
			String issuerCode,
			String acquirerCode,
			String number,
			Integer installmentPlanMonths,
			Boolean isInterestFree,
			String cardType,
			String ownerType,
			Long amount
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Receipt(
			String url
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Failure(
			String code,
			String message
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Cancel(
			String cancelReason,
			String canceledAt,
			Long cancelAmount,
			Long taxFreeAmount,
			Long refundableAmount,
			String transactionKey,
			String receiptKey,
			String cancelStatus
	) {}
}