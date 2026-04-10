package com.goorm.roomflow.domain.reservation.dto.response;

import java.time.LocalDateTime;

public record ReservationTimeSlot(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public String timeRange() {
        return String.format("%d:00 - %d:00",
                startAt.getHour(),
                endAt.getHour());
    }
}
