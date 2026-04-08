package com.goorm.roomflow.domain.holiday.dto.response;

import java.time.LocalDate;
import java.util.List;

public record HolidayCalendarRes(
        String holidayDate,
        String holidayName
) {
}
