package com.goorm.roomflow.domain.equipment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentStatus {
	AVAILABLE("사용 가능"),
	MAINTENANCE("점검중"),
	INACTIVE("비활성");

	private final String description;
}