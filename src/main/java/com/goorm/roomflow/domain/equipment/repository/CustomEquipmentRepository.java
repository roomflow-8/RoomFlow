package com.goorm.roomflow.domain.equipment.repository;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomEquipmentRepository {

	/**
	 * 예약 ID로 사용 가능한 비품 목록 조회
	 * @param reservationId 예약 ID
	 * @return 사용 가능한 비품 목록 (재고 계산 포함)
	 */
	List<EquipmentAvailabilityDto> findAvailableEquipmentsByReservation(Long reservationId);


}
