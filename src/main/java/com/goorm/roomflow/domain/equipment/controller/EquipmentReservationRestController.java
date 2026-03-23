package com.goorm.roomflow.domain.equipment.controller;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.service.EquipmentService;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Equipment API", description = "비품 조회 API")
public class EquipmentReservationRestController {

	private final EquipmentService equipmentService;

	@Operation(summary = "비품 조회", description = "해당 시간에 대여 가능한 비품을 조회합니다.")
	@GetMapping("/reservations/{reservationId}/equipments")
	public ResponseEntity<ApiResponse<List<EquipmentAvailabilityDto>>> getAvailableEquipments(
			@PathVariable Long reservationId) {

		log.info("GET /api/reservations/{}/equipments", reservationId);

		List<EquipmentAvailabilityDto> availableEquipments =
				equipmentService.getAvailableEquipments(reservationId);

		return ApiResponse.success(SuccessCode.EQUIPMENT_SUCCESS, availableEquipments);
	}
}
