package com.goorm.roomflow.domain.reservation.dto.request;

import com.goorm.roomflow.domain.equipment.dto.request.EquipmentReservationReq;

import java.util.List;

public record CreateReservationRoomReq(

		Long roomId,
		List<Long> roomSlotIds,
		String idempotencyKey
) {
}
