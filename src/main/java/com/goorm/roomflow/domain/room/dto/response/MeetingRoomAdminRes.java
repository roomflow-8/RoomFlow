package com.goorm.roomflow.domain.room.dto.response;

import com.goorm.roomflow.domain.room.entity.RoomStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MeetingRoomAdminRes(
        Long roomId,
        String roomName,
        int capacity,
        String description,
        BigDecimal hourlyPrice,
        RoomStatus status,
        String imageUrl,
        int totalReservations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
