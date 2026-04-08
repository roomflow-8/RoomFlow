package com.goorm.roomflow.domain.room.dto.request;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminRoomSlotsReq(
        LocalDate targetDate,
        List<Long> meetingRoomIds
) {
}
