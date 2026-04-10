package com.goorm.roomflow.domain.equipment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentStatus {
	AVAILABLE("사용 가능", 0),
	MAINTENANCE("점검중", 2),
	INACTIVE("비활성", 3);

	private final String description;
	private final int priority;

	public int getOrder() {
		return priority;
	}
}