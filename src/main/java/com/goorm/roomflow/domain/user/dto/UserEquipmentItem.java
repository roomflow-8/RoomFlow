package com.goorm.roomflow.domain.user.dto;

import java.math.BigDecimal;

public record UserEquipmentItem(
        Long reservationEquipmentId,  // 비품 예약 ID
        String equipmentName,
        int quantity,
        BigDecimal totalAmount
) {}
