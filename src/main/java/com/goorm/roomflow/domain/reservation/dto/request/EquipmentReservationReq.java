package com.goorm.roomflow.domain.reservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 비품 예약 요청 DTO - Record
 */
public record EquipmentReservationReq(

		@NotNull(message = "비품 ID는 필수입니다.")
		Long equipmentId,

		@NotNull(message = "수량은 필수입니다.")
		@Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
		Integer quantity,

		BigDecimal unitPrice
) {
	// Compact Constructor (유효성 검증 추가 가능)
	public EquipmentReservationReq {
		// 추가 검증 로직 (선택사항)
		if (equipmentId != null && equipmentId <= 0) {
			throw new IllegalArgumentException("비품 ID는 양수여야 합니다.");
		}
	}

}