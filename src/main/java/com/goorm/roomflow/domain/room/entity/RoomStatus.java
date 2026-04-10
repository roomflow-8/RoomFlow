package com.goorm.roomflow.domain.room.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomStatus {
	AVAILABLE("예약 가능", 0),
	MAINTENANCE("점검 중", 2),
	INACTIVE("비활성화", 3);

	private final String label;
	private final int priority;

	public int getOrder() {
		return priority;
	}
}