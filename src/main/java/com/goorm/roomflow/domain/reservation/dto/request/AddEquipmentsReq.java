package com.goorm.roomflow.domain.reservation.dto.request;

import com.goorm.roomflow.domain.equipment.dto.request.EquipmentReservationReq;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddEquipmentsReq(
		@NotEmpty(message = "비품을 선택해주세요.")
		@Valid
		List<EquipmentReservationReq> equipments
) {
}
