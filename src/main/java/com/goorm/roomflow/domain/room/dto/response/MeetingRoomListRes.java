package com.goorm.roomflow.domain.room.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record MeetingRoomListRes(
        LocalDate date,
        long availableRoomCount,
        List<MeetingRoomRes> rooms
) {
}
