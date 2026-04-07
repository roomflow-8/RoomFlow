package com.goorm.roomflow.domain.room.dto.response;

import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDateTime;

public record RoomSlotAdminRes(
        Long roomSlotId,
        LocalDateTime slotStartAt,
        LocalDateTime slotEndAt,
        boolean active,
        boolean reserved
) {
}