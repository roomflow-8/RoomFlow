package com.goorm.roomflow.domain.room.dto.response;

import com.goorm.roomflow.domain.room.entity.RoomStatus;

import java.math.BigDecimal;
import java.util.List;

public record MeetingRoomRes(
        Long roomId,
        String roomName,
        int capacity,
        String description,
        BigDecimal hourlyPrice,
        RoomStatus status,
        String statusMessage,
        String imageUrl,
        int totalReservations,
        List<RoomSlotRes> roomSlots
) {
    public String priceText() {
        return String.format("%,d/시간", hourlyPrice.intValue());
    }
}
