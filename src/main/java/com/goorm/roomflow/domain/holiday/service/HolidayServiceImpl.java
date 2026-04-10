package com.goorm.roomflow.domain.holiday.service;

import com.goorm.roomflow.domain.holiday.dto.response.HolidayCalendarRes;
import com.goorm.roomflow.domain.holiday.entity.Holiday;
import com.goorm.roomflow.domain.holiday.repository.HolidayRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    /**
     * 달력 비활성화용 휴무일 목록 조회
     */
    @Override
    public List<HolidayCalendarRes> getHolidayDatesForCalendar(LocalDate startDate, LocalDate endDate) {
        return holidayRepository
                .findByHolidayDateBetweenAndIsActiveTrue(startDate, endDate)
                .stream()
                .map(h -> new HolidayCalendarRes(
                        h.getHolidayDate().toString(),
                        h.getTitle()
                ))
                .toList();
    }

    public boolean isHoliday(LocalDate reservationDate) {
        return holidayRepository.existsByHolidayDateAndIsActiveTrue(reservationDate);
    }
}
