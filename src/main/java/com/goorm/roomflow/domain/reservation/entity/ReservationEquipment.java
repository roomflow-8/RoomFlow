package com.goorm.roomflow.domain.reservation.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.equipment.entity.Equipment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
		name = "reservation_equipments",
		uniqueConstraints = {
				@UniqueConstraint(
						columnNames = {"reservation_id", "equipment_id"}
				)
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReservationEquipment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reservationEquipmentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "equipment_id", nullable = false)
	private Equipment equipment;

	@Column(nullable = false)
	private int quantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus status;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal totalAmount;

	private LocalDateTime cancelledAt;

	private String cancelReason;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Builder
	public ReservationEquipment(Reservation reservation, Equipment equipment, int quantity, ReservationStatus status,
								BigDecimal unitPrice, BigDecimal totalAmount) {
		// 수량 검증 (CHECK 제약조건)
		if (quantity <= 0) {
			throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
		}

		this.reservation = reservation;
		this.equipment = equipment;
		this.quantity = quantity;
		this.status = (status != null) ? status : ReservationStatus.PENDING;
		this.unitPrice = (unitPrice != null) ? unitPrice : BigDecimal.ZERO;
		this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(quantity)); //  ||    this.totalAmount = totalAmount;
	}

	// --- 비즈니스 로직 ---
	public void cancel(String reason) {
		this.status = ReservationStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
		this.cancelReason = reason;
	}

	public void confirm() {
		if (this.status != ReservationStatus.PENDING) {
			throw new IllegalStateException("PENDING 상태만 확정할 수 있습니다.");
		}
		this.status = ReservationStatus.CONFIRMED;
	}

	public void expire() {
		this.status = ReservationStatus.EXPIRED;
	}

}