package com.goorm.roomflow.domain.room.dto.response;

import com.goorm.roomflow.domain.room.entity.RoomStatus;

import java.time.LocalDate;

public record AdminRoomSlotSummaryRes(
        Long roomId,
        String roomName,
        RoomStatus roomStatus,
        LocalDate selectedDate,
        int totalSlotCount,
        int reservedSlotCount,
        int availableSlotCount,
        int inactiveSlotCount
) {
}
