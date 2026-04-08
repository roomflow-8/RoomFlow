package com.goorm.roomflow.domain.room.dto.request;

import com.goorm.roomflow.domain.room.entity.RoomStatus;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public record AdminMeetingRoomReq(
        String roomName,
        int capacity,
        String description,
        BigDecimal hourlyPrice,
        RoomStatus status,
        MultipartFile imageFile
) {
}
