package com.goorm.roomflow.domain.room.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record RoomSlotDateStatusReq(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,
        boolean active
) {
}
