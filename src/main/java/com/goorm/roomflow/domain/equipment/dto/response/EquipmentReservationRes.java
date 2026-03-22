package com.goorm.roomflow.domain.equipment.dto.response;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;

import java.math.BigDecimal;
import java.util.List;

import static com.goorm.roomflow.domain.reservation.entity.QReservation.reservation;

public record EquipmentReservationRes(
		Long reservationId,
		List<EquipmentItem> equipments,
		BigDecimal totalAmount
) {
	public record EquipmentItem(
			Long reservationEquipmentId,
			Long equipmentId,
			String equipmentName,
			Integer quantity,
			BigDecimal unitPrice,
			BigDecimal totalAmount,
			String status
	) {
		// 정적 팩토리 메서드 추가
		public static EquipmentItem from(ReservationEquipment entity) {
			return new EquipmentItem(
					entity.getReservationEquipmentId(),
					entity.getEquipment().getEquipmentId(),
					entity.getEquipment().getEquipmentName(),
					entity.getQuantity(),
					entity.getUnitPrice(),
					entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())),
					entity.getEquipment().getStatus().name()
			);
		}
	}

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
