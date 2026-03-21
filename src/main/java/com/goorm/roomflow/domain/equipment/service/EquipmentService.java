package com.goorm.roomflow.domain.equipment.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EquipmentService {
	private final EquipmentRepository equipmentRepository;

	/**
	 * 예약에 사용 가능한 장비 목록 조회
	 * @param reservationId 예약 ID
	 * @return 사용 가능한 장비 목록
	 */
	public List<EquipmentAvailabilityDto> getAvailableEquipments(Long reservationId) {
		log.info("예약 ID {}에 대한 사용 가능한 장비 조회 시작", reservationId);

		try {
			List<EquipmentAvailabilityDto> availableEquipments =
					equipmentRepository.findAvailableEquipmentsByReservation(reservationId);

			log.info("조회된 장비 개수: {}", availableEquipments.size());

			return availableEquipments;
		} catch (Exception e) {
			log.error("==============", e);
			throw e;
		}
	}
}
