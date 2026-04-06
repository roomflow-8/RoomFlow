package com.goorm.roomflow.domain.room.dto.request;

import com.goorm.roomflow.domain.room.entity.RoomStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MeetingRoomReq(
        @NotBlank(message = "회의실 이름은 필수입니다.")
        String roomName,

        @Min(value = 1, message = "회의실 정원은 1 이상이어야 합니다.")
        Integer capacity,

        String description,

        @NotNull(message = "회의실 요금은 필수입니다.")
        @DecimalMin(value = "0", message = "회의실 요금은 0 이상이어야 합니다.")
        BigDecimal hourlyPrice,

        @NotNull(message = "회의실 상태는 필수입니다.")
        RoomStatus status
){

}
