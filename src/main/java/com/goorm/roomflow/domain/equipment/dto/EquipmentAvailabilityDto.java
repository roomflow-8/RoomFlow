package com.goorm.roomflow.domain.equipment.dto;

import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.querydsl.core.annotations.QueryProjection;

import java.math.BigDecimal;

public record EquipmentAvailabilityDto(
		Long equipmentId,
		String equipmentName,
		String imageUrl,
		Integer totalStock,
		Integer availableStock,
		EquipmentStatus status,
		BigDecimal price
) {
	@QueryProjection
	public EquipmentAvailabilityDto{
	}
}
