package com.goorm.roomflow.domain.room.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminRoomSlotGenerateReq(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
) {
}