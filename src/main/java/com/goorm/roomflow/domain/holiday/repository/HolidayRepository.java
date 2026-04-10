package com.goorm.roomflow.domain.holiday.repository;

import com.goorm.roomflow.domain.holiday.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findAllByOrderByHolidayDateAsc();
    List<Holiday> findByHolidayDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate);

    @Query("""
        select count(h) > 0
        from Holiday h
        where h.holidayDate = :holidayDate
          and (:holidayId is null or h.holidayId <> :holidayId)
    """)
    boolean existsDuplicateHoliday(LocalDate holidayDate, Long holidayId);

    boolean existsByHolidayDateAndIsActiveTrue(LocalDate holidayDate);
}
