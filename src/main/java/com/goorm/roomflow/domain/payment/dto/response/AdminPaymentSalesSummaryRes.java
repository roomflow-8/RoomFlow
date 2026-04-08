package com.goorm.roomflow.domain.payment.dto.response;

import java.math.BigDecimal;

public record AdminPaymentSalesSummaryRes(
        BigDecimal totalSales,
        BigDecimal roomSales,
        BigDecimal equipmentSales,
        long doneCount
) {
}
