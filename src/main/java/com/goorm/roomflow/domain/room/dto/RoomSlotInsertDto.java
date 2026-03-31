package com.goorm.roomflow.domain.room.dto;

import java.time.LocalDateTime;

public record RoomSlotInsertDto(
        Long roomId,
        LocalDateTime slotStartAt,
        LocalDateTime slotEndAt,
        boolean isActive
) {
}