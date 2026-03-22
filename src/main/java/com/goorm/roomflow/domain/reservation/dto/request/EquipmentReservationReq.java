package com.goorm.roomflow.domain.reservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class EquipmentReservationReq {

	@NotNull(message = "비품 ID는 필수입니다.")
	private Long equipmentId;

	@NotNull(message = "수량은 필수입니다.")
	@Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
	private Integer quantity;

	private BigDecimal unitPrice;
}
