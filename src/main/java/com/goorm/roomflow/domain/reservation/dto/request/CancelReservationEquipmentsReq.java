package com.goorm.roomflow.domain.reservation.dto.request;

import java.util.List;

public record CancelReservationEquipmentsReq(
		List<Long> reservationEquipmentIds,
		String reason

) {
}
