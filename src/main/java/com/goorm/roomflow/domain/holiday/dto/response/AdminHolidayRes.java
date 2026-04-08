package com.goorm.roomflow.domain.holiday.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminHolidayRes(
        Long holidayId,
        String title,
        String description,
        LocalDate holidayDate,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
