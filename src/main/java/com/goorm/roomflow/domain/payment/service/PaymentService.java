package com.goorm.roomflow.domain.payment.service;

import com.goorm.roomflow.domain.payment.dto.request.PaymentCheckoutReq;
import com.goorm.roomflow.domain.payment.dto.response.PaymentResultRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;

import java.math.BigDecimal;

public interface PaymentService {

	PaymentCheckoutReq createCheckoutInfo(ReservationRoomRes reservationRoom);

	// 결제 요청 저장 (checkout 페이지 진입 시)
	void savePaymentRequest(Long userId, Long reservationId,
							String orderId, String orderName,
							BigDecimal roomAmount, BigDecimal equipmentAmount);

	// 결제 승인 처리 (success 콜백)
	PaymentResultRes confirmPayment(String orderId, String paymentKey, Long amount);

	// 결제 실패 처리 (fail 콜백)
	void failPayment(String orderId, String code, String message);
}
