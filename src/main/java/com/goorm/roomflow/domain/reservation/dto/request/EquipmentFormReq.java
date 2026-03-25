package com.goorm.roomflow.domain.reservation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 비품 추가 페이지 Form 데이터 바인딩용 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class EquipmentFormReq {

	private List<EquipmentFormItem> equipments = new ArrayList<>();

	/**
	 * 내부 클래스: 개별 비품 정보
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	public static class EquipmentFormItem {
		private Long equipmentId;
		private Integer quantity;
	}


	/**
	 * AddEquipmentsReq로 변환
	 */
	public AddEquipmentsReq toAddEquipmentsReq() {
		List<EquipmentReservationReq> requests = equipments.stream()
				.filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
				.map(item -> new EquipmentReservationReq(
						item.getEquipmentId(),
						item.getQuantity(),
						null
				))
				.toList();

		return new AddEquipmentsReq(requests);
	}

	/**
	 * 선택된 비품이 있는지 확인
	 */
	public boolean hasEquipments() {
		return equipments.stream()
				.anyMatch(item -> item.getQuantity() != null && item.getQuantity() > 0);
	}
}