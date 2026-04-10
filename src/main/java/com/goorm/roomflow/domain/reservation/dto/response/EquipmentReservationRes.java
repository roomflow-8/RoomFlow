package com.goorm.roomflow.domain.reservation.dto.response;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;

import java.math.BigDecimal;
import java.util.List;

public record EquipmentReservationRes(
		Long reservationId,
		List<EquipmentItem> equipments,
		BigDecimal totalAmount
) {
	// 전체 변환 메서드 추가
	public static EquipmentReservationRes from(Reservation reservation, List<ReservationEquipment> equipments) {
		return new EquipmentReservationRes(
				reservation.getReservationId(),
				equipments.stream()
						.map(EquipmentItem::from)
						.toList(),
				reservation.getTotalAmount()
		);
	}
}
