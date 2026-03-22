package com.goorm.roomflow.domain.equipment.dto;

import com.goorm.roomflow.domain.equipment.entity.EquipmentStatus;
import com.querydsl.core.annotations.QueryProjection;

import java.math.BigDecimal;

public record EquipmentAvailabilityDto(
		Long equipmentId,
		String equipmentName,
		String imageUrl,
		Integer totalStock,
		Integer availableStock, //대여 가능 수량
		EquipmentStatus status,
		BigDecimal price
) {
	@QueryProjection
	public EquipmentAvailabilityDto
			(
					Long equipmentId,
					String equipmentName,
					String imageUrl,
					Integer totalStock,
					Integer availableStock,
					EquipmentStatus status,
					BigDecimal price
			) {
		this.equipmentId = equipmentId;
		this.equipmentName = equipmentName;
		this.imageUrl = imageUrl;
		this.totalStock = totalStock;
		this.availableStock = Math.max(0, availableStock != null ? availableStock : 0);
		this.status = status;
		this.price = price;
	}
}
