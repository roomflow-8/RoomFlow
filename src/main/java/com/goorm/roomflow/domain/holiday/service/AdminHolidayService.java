package com.goorm.roomflow.domain.holiday.service;

import com.goorm.roomflow.domain.holiday.dto.request.AdminHolidayReq;
import com.goorm.roomflow.domain.holiday.dto.response.AdminHolidayRes;

import java.time.LocalDate;
import java.util.List;

public interface AdminHolidayService {
    List<AdminHolidayRes> getHolidayList();

    Long createHoliday(AdminHolidayReq request);

    void modifyHoliday(Long holidayId, AdminHolidayReq request);

    void deleteHoliday(Long holidayId);

    void changeHolidayStatus(Long holidayId, boolean active);

    void validateHoliday(LocalDate reservationDate);


    int importPublicHolidays(int year, Integer month);
}
