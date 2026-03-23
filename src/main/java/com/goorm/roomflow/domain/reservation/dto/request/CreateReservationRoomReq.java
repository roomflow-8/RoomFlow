package com.goorm.roomflow.domain.reservation.dto.request;

import java.util.List;

public record CreateReservationRoomReq(

		Long roomId,
		List<Long> roomSlotIds,
		String idempotencyKey
) {
}
