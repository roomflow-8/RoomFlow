package com.goorm.roomflow.domain.holiday.dto.request;

import java.time.LocalDate;

public record AdminHolidayReq(
        String title,
        String description,
        LocalDate holidayDate,
        Boolean active
) {}
