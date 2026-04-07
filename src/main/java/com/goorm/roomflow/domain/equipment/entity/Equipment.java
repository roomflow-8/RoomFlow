package com.goorm.roomflow.domain.equipment.entity;

import com.goorm.roomflow.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Equipment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long equipmentId;

	@Column(nullable = false, length = 100)
	private String equipmentName;

	@Column(nullable = false)
	private int totalStock = 0;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private int maintenanceLimit = 0;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EquipmentStatus status = EquipmentStatus.AVAILABLE;

	@Column(nullable = false)
	private int totalReservations = 0;

	@Column(length = 1000)
	private String imageUrl;

	@Builder
	public Equipment(String equipmentName, int totalStock, String description,
					 int maintenanceLimit, BigDecimal price, EquipmentStatus status,
					 int totalReservations, String imageUrl) {

		validate(equipmentName, totalStock, maintenanceLimit, price);

		this.equipmentName = equipmentName;
		this.totalStock = totalStock;
		this.description = description;
		this.maintenanceLimit = maintenanceLimit;
		this.price = (price != null) ? price : BigDecimal.ZERO;
		this.status = (status != null) ? status : EquipmentStatus.AVAILABLE;
		this.totalReservations = totalReservations;
		this.imageUrl = imageUrl;
	}

	// --- 비즈니스 로직 ---
	public void updateStock(int newStock) {
		if (newStock < 0) throw new IllegalArgumentException("재고 부족");
		this.totalStock = newStock;
	}

	public void update(
			String equipmentName,
			int totalStock,
			String description,
			int maintenanceLimit,
			BigDecimal price,
			EquipmentStatus status,
			String imageUrl
	) {
		validate(equipmentName, totalStock, maintenanceLimit, price);

		this.equipmentName = equipmentName;
		this.totalStock = totalStock;
		this.description = description;
		this.maintenanceLimit = maintenanceLimit;
		this.price = price;
		this.imageUrl = imageUrl;

		changeStatus(status);
	}

	public void incrementReservations() {
		this.totalReservations++;
	}
	public void decrementReservations() {
		if (this.totalReservations <= 0) {
			throw new IllegalStateException("예약 건수가 0 이하일 수 없습니다.");
		}
		this.totalReservations--;
	}

	public void changeStatus(EquipmentStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("비품 상태는 필수입니다.");
		}
		this.status = status;
	}

	private void validate(
			String equipmentName,
			int totalStock,
			int maintenanceLimit,
			BigDecimal price
	) {
		validateEquipmentName(equipmentName);
		validateTotalStock(totalStock);
		validateMaintenanceLimit(maintenanceLimit);
		validatePrice(price);
	}

	private void validateEquipmentName(String equipmentName) {
		if (equipmentName == null || equipmentName.isBlank()) {
			throw new IllegalArgumentException("비품 이름은 필수입니다.");
		}
	}

	private void validateTotalStock(int totalStock) {
		if (totalStock < 0) {
			throw new IllegalArgumentException("비품 개수는 0 이상이어야 합니다.");
		}
	}

	private void validateMaintenanceLimit(int maintenanceLimit) {
		if (maintenanceLimit < 0) {
			throw new IllegalArgumentException("점검 기준 개수는 0 이상이어야 합니다.");
		}
	}

	private void validatePrice(BigDecimal price) {
		if (price == null) {
			throw new IllegalArgumentException("비품 요금은 필수입니다.");
		}
		if (price.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("비품 요금은 0 이상이어야 합니다.");
		}
	}

}