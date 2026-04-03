package com.goorm.roomflow.domain.room.dto.response;

import com.goorm.roomflow.domain.room.entity.RoomStatus;

import java.math.BigDecimal;

public record MeetingRoomAdminRes(
        Long roomId,
        String roomName,
        int capacity,
        String description,
        BigDecimal hourlyPrice,
        RoomStatus status,
        String imageUrl,
        int totalReservations
) {
}
