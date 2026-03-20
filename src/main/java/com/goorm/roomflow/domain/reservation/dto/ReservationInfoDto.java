package com.goorm.roomflow.domain.reservation.dto;

import java.time.LocalDateTime;

public record ReservationInfoDto(
		Long reservationId,
		Long roomId,
		String meetingRoomName,
		LocalDateTime startAt,
		LocalDateTime endAt
) {

}
