package com.goorm.roomflow.domain.room.dto.request;

import com.goorm.roomflow.domain.room.entity.RoomStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public record MeetingRoomReq(
        String roomName,
        int capacity,
        String description,
        BigDecimal hourlyPrice,
        RoomStatus status,
        MultipartFile imageFile
) {
}
