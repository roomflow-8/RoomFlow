package com.goorm.roomflow.domain.room.dto.response;

import java.util.List;

public record RoomSlotAdminPageRes(
        RoomSlotSummaryRes summary,
        List<RoomSlotAdminRes> slots
) {
}