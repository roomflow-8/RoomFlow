package com.goorm.roomflow.domain.room.dto.response;

import java.time.LocalDateTime;

public record AdminRoomSlotRes(
        Long roomSlotId,
        LocalDateTime slotStartAt,
        LocalDateTime slotEndAt,
        boolean active,
        boolean reserved
) {
}