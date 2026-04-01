package com.goorm.roomflow.domain.reservation.dto.request;

import java.util.List;

public record CancelReservationReq(
        String reason,
        List<Long> reservationEquipmentIds
) {
}
