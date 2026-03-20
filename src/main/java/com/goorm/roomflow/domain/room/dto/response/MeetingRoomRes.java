package com.goorm.roomflow.domain.room.dto.response;

import com.goorm.roomflow.domain.room.entity.RoomStatus;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

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
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(hourlyPrice) + "원";
    }
}
