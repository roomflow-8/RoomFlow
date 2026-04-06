package com.goorm.roomflow.domain.equipment.controller;

import com.goorm.roomflow.domain.equipment.service.EquipmentService;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping()
public class EquipmentController {

	private final EquipmentService equipmentService;
	private final ReservationService reservationService;




}
