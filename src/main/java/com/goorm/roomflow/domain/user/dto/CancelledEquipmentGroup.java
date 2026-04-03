package com.goorm.roomflow.domain.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CancelledEquipmentGroup(
        LocalDateTime cancelledAt,   // 취소 시각
        String cancelReason,         // 취소 사유
        List<UserEquipmentItem> items, // 해당 이벤트에서 취소된 비품 목록
        BigDecimal totalAmount       // 해당 이벤트 취소 비품 합산 금액
) {}
