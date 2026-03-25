package com.goorm.roomflow.domain.reservation.dto.response;

import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import java.math.BigDecimal;

public record EquipmentItem(
        Long reservationEquipmentId,
        Long equipmentId,
        String equipmentName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status
) {
    // 정적 팩토리 메서드 추가
    public static EquipmentItem from(ReservationEquipment entity) {
        return new EquipmentItem(
                entity.getReservationEquipmentId(),
                entity.getEquipment().getEquipmentId(),
                entity.getEquipment().getEquipmentName(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())),
                entity.getEquipment().getStatus().name()
        );
    }
}