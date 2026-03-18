package com.goorm.roomflow.domain.equipment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Equipment {

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
	private BigDecimal price = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EquipmentStatus status = EquipmentStatus.AVAILABLE;

	@Column(nullable = false)
	private int totalReservations = 0;

	@Column(length = 1000)
	private String imageUrl;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public Equipment(String equipmentName, int totalStock, String description,
					 int maintenanceLimit, BigDecimal price, EquipmentStatus status, String imageUrl) {

		// SQL의 CHECK 제약조건 (total_stock >= 0) 검증
		if (totalStock < 0) {
			throw new IllegalArgumentException("재고는 0보다 작을 수 없습니다.");
		}

		this.equipmentName = equipmentName;
		this.totalStock = totalStock;
		this.description = description;
		this.maintenanceLimit = maintenanceLimit;
		this.price = (price != null) ? price : BigDecimal.ZERO;
		this.status = (status != null) ? status : EquipmentStatus.AVAILABLE;
		this.imageUrl = imageUrl;
	}

	// --- 비즈니스 로직 ---
	public void updateStock(int newStock) {
		if (newStock < 0) throw new IllegalArgumentException("재고 부족");
		this.totalStock = newStock;
	}

	public void changeStatus(EquipmentStatus status) {
		this.status = status;
	}
}