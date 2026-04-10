package com.goorm.roomflow.domain.equipment.dto.response;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record EquipmentListRes(
		String roomName,                      // 회의실 이름
		LocalDate date,                       // 예약 날짜
		LocalTime startAt,                    // 시작 시간
		LocalTime endAt,                      // 종료 시간
		List<EquipmentAvailabilityDto> list   // 비품 목록
) {}