package com.goorm.roomflow.domain.payment.service;

import com.goorm.roomflow.domain.payment.dto.response.AdminPaymentSalesRes;

public interface AdminPaymentService {
    AdminPaymentSalesRes getSalesPage();
    byte[] exportSalesExcel();
}
