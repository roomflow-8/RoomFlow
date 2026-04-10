package com.goorm.roomflow.domain.holiday.service;

import com.goorm.roomflow.domain.holiday.dto.request.AdminHolidayReq;
import com.goorm.roomflow.domain.holiday.dto.response.AdminHolidayRes;
import com.goorm.roomflow.domain.holiday.entity.Holiday;
import com.goorm.roomflow.domain.holiday.mapper.HolidayMapper;
import com.goorm.roomflow.domain.holiday.repository.HolidayRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHolidayServiceImpl implements AdminHolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;
    private final AdminHolidayPublicApiService adminHolidayPublicApiService;

    /**
     * 휴무일 목록 조회
     */
    @Override
    public List<AdminHolidayRes> getHolidayList() {
        return holidayRepository.findAllByOrderByHolidayDateAsc()
                .stream()
                .map(holidayMapper::toAdminHolidayRes)
                .toList();
    }

    /**
     * 휴무일 등록
     */
    @Override
    @Transactional
    public Long createHoliday(AdminHolidayReq request) {
        validateDuplicate(request.holidayDate(), null);

        Holiday holiday = Holiday.builder()
                .title(request.title())
                .description(request.description())
                .holidayDate(request.holidayDate())
                .isActive(request.active())
                .build();

        Holiday savedHoliday = holidayRepository.save(holiday);

        log.info("휴무일 등록 완료 - holidayId={}", savedHoliday.getHolidayId());

        return savedHoliday.getHolidayId();
    }

    /**
     * 휴무일 수정
     */
    @Override
    @Transactional
    public void modifyHoliday(Long holidayId, AdminHolidayReq request) {
        Holiday holiday = getHolidayEntity(holidayId);
        validateDuplicate(request.holidayDate(), holidayId);

        holiday.update(
                request.title(),
                request.description(),
                request.holidayDate(),
                request.active()
        );

        log.info("휴무일 수정 완료 - holidayId={}", holiday.getHolidayId());
    }

    /**
     * 휴무일 삭제
     */
    @Transactional
    public void deleteHoliday(Long holidayId) {
        Holiday holiday = getHolidayEntity(holidayId);
        holidayRepository.delete(holiday);

        log.info("휴무일 삭제 완료 - holidayId={}", holidayId);
    }

    /**
     * 휴무일 활성화/비활성화
     */
    @Transactional
    public void changeHolidayStatus(Long holidayId, boolean active) {
        Holiday holiday = getHolidayEntity(holidayId);

        holiday.changeActive(active);

        log.info("휴무일 상태 변경 완료 - holidayId={}, active={}", holidayId, active);
    }


    /**
     * 예약 가능 날짜 검증
     */
    @Override
    public void validateHoliday(LocalDate reservationDate) {
        if (holidayRepository.existsByHolidayDateAndIsActiveTrue(reservationDate)) {
            throw new BusinessException(ErrorCode.HOLIDAY_RESERVATION_NOT_ALLOWED);
        }
    }


    /**
     * 공공 API 휴무일 가져오기
     */
    @Override
    @Transactional
    public int importPublicHolidays(int year, Integer month) {
        List<AdminHolidayReq> publicHolidays = adminHolidayPublicApiService.getPublicHolidays(year, month);

        int savedCount = 0;

        for (AdminHolidayReq request : publicHolidays) {
            if (holidayRepository.existsDuplicateHoliday(request.holidayDate(), null)) {
                continue;
            }

            Holiday holiday = Holiday.builder()
                    .title(request.title())
                    .description(request.description())
                    .holidayDate(request.holidayDate())
                    .isActive(request.active())
                    .build();
            holidayRepository.save(holiday);
            savedCount++;
        }

        log.info("공공 API 휴무일 가져오기 완료 - year={}, month={}, savedCount={}",
                year, month, savedCount);

        return savedCount;
    }

    private Holiday getHolidayEntity(Long holidayId) {
        return holidayRepository.findById(holidayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLIDAY_NOT_FOUND));
    }

    private void validateDuplicate(LocalDate holidayDate, Long holidayId) {
        if (holidayRepository.existsDuplicateHoliday(holidayDate, holidayId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_HOLIDAY);
        }
    }
}
