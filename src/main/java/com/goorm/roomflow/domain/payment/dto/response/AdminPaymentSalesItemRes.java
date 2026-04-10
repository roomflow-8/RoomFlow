package com.goorm.roomflow.domain.payment.dto.response;

import com.goorm.roomflow.domain.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentSalesItemRes(
        Long paymentId,
        String orderId,
        String orderName,
        String userName,
        PaymentStatus status,
        BigDecimal roomAmount,
        BigDecimal equipmentAmount,
        BigDecimal totalAmount,
        String method,
        String receiptUrl,
        LocalDateTime approvedAt
) {
}
