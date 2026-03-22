package com.goorm.roomflow.domain.equipment.controller;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.service.EquipmentService;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class EquipmentController {

	private final EquipmentService equipmentService;

	/**
	 * 비품 선택 페이지
	 * @param reservationId 예약 ID
	 * @param model 모델
	 * @return 비품 선택 페이지
	 */
	@GetMapping("/{reservationId}/equipments")
	public String readAvailableEquipments(Long reservationId, Model model) {
		log.info("비품 선택 페이지 요청 - reservationId: {}", reservationId);
		model.addAttribute("reservationId", reservationId);
		return "equipment/list";
	}


}
