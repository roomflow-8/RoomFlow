package com.goorm.roomflow.domain.holiday.service;

import com.goorm.roomflow.domain.holiday.dto.response.HolidayCalendarRes;

import java.time.LocalDate;
import java.util.List;

public interface HolidayService {
    List<HolidayCalendarRes> getHolidayDatesForCalendar(LocalDate startDate, LocalDate endDate);
}
