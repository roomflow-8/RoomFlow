package com.goorm.roomflow.domain.room.dto.response;

import java.util.List;

public record AdminRoomSlotPageRes(
        AdminRoomSlotSummaryRes summary,
        List<AdminRoomSlotRes> slots
) {
}