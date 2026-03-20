package com.goorm.roomflow.domain.equipment.controller;

import com.goorm.roomflow.domain.equipment.dto.EquipmentListRes;
import com.goorm.roomflow.domain.equipment.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EquipmentReservationRestController {

	private final EquipmentService equipmentService;

//	@GetMapping("/reservations/{reservationId}/equipments")
//	public ResponseEntity<EquipmentListRes> getAvailableEquipments(Long reservationId){
//		EquipmentListRes equipmentListRes = equipmentService.getAvailableEquipments(reserationId);
//		return ResponseEntity.ok(equipmentListRes);
//	}
}
