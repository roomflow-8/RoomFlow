package com.goorm.roomflow.domain.payment.dto.response;

import java.util.List;

public record AdminPaymentSalesRes(
        AdminPaymentSalesSummaryRes summary,
        List<AdminPaymentSalesItemRes> payments
) {
}
