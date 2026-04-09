package com.goorm.roomflow.domain.reservation.dto.response;

import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import java.math.BigDecimal;

/**
 * 예약 비품 Response
 * 관리자/사용자 공통으로 사용되는 예약 비품 정보
 */
public record ReservationEquipmentRes(
		Long reservationEquipmentId,                    // 예약비품 매핑 ID
		Long equipmentId,           // 비품 ID
		String name,                // 비품명
		String description,         // 비품 설명
		Integer quantity,           // 수량
		Integer price,              // 단가
		Integer totalAmount          // 총 가격
) {

	// Compact Constructor (유효성 검증)
	public ReservationEquipmentRes {
		if (quantity == null) {
			quantity = 0;
		}
		if (price == null) {
			price = 0;
		}
		if (totalAmount == null) {
			totalAmount = price * quantity;
		}
	}

	// 표시용 메서드
	public String getDisplayName() {
		if (name != null && quantity != null) {
			return name + " × " + quantity;
		}
		return name != null ? name : "";
	}

	public String getFormattedPrice() {
		if (price != null) {
			return String.format("%,d원", price);
		}
		return "0원";
	}

	public String getFormattedtotalAmount() {
		if (totalAmount != null) {
			return String.format("%,d원", totalAmount);
		}
		return "0원";
	}

	// 정적 팩토리 메서드 (Entity → DTO)
	public static ReservationEquipmentRes from(ReservationEquipment entity) {
		if (entity == null) {
			return null;
		}

		return new ReservationEquipmentRes(
				entity.getReservationEquipmentId(),
				entity.getEquipment().getEquipmentId(),
				entity.getEquipment().getEquipmentName(),
				entity.getEquipment().getDescription(),
				entity.getQuantity(),
				entity.getUnitPrice() != null ? entity.getUnitPrice().intValue() : 0,
				entity.getUnitPrice() != null
						? entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())).intValue()
						: 0
		);
	}

	// Builder 패턴
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Long reservationEquipmentId;
		private Long equipmentId;
		private String name;
		private String description;
		private Integer quantity;
		private Integer price;
		private Integer totalAmount;

		public Builder reservationEquipmentId(Long reservationEquipmentId) {
			this.reservationEquipmentId = reservationEquipmentId;
			return this;
		}

		public Builder equipmentId(Long equipmentId) {
			this.equipmentId = equipmentId;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder quantity(Integer quantity) {
			this.quantity = quantity;
			return this;
		}

		public Builder price(Integer price) {
			this.price = price;
			return this;
		}

		public Builder totalAmount(Integer totalAmount) {
			this.totalAmount = totalAmount;
			return this;
		}

		public ReservationEquipmentRes build() {
			return new ReservationEquipmentRes(
					reservationEquipmentId, equipmentId, name, description,
					quantity, price, totalAmount
			);
		}
	}
}