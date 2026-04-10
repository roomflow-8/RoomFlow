package com.goorm.roomflow.domain.room.dto.response;

import java.time.LocalDateTime;

public record RoomSlotRes(

        Long roomSlotId,
        LocalDateTime slotStartAt,
        LocalDateTime slotEndAt,
        boolean isActive
) {
    public String timeRange() {
        return String.format("%d:00 - %d:00",
                slotStartAt.getHour(),
                slotEndAt.getHour());
    }
}
