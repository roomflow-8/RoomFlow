package com.goorm.roomflow.domain.equipment.dto.response;

import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminEquipmentRes(
        Long equipmentId,
        String equipmentName,
        Integer totalStock,
        String description,
        Integer maintenanceLimit,
        BigDecimal price,
        EquipmentStatus status,
        String imageUrl,
        int totalReservations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
