package com.goorm.roomflow.domain.equipment.controller;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.service.EquipmentService;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class EquipmentReservationRestController {

	private final EquipmentService equipmentService;

	@GetMapping("/reservations/{reservationId}/equipments")
	public ResponseEntity<ApiResponse<List<EquipmentAvailabilityDto>>> getAvailableEquipments(
			@PathVariable Long reservationId) {

		log.info("------------------------------");
		log.info("GET /api/reservations/{}/equipments", reservationId);
		log.info("------------------------------");

		List<EquipmentAvailabilityDto> availableEquipments =
				equipmentService.getAvailableEquipments(reservationId);

		return ApiResponse.success(SuccessCode.EQUIPMENT_SUCCESS, availableEquipments);
	}
}
