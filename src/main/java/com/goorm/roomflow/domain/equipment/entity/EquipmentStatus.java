package com.goorm.roomflow.domain.equipment.entity;
//
//TODO: 차이점 확인
//public enum EquipmentStatus {
//	AVAILABLE, MAINTENANCE, INACTIVE
//}

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