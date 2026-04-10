package com.goorm.roomflow.domain.reservation.entity;

import com.goorm.roomflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReservationHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long historyId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TargetType targetType;

	@Column(nullable = false)
	private Long targetId; // targetType에 따라 reservationId 또는 reservationEquipmentId 저장

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus toStatus;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "changed_by")
	private User changedBy;

	private String reason;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@Builder
	public ReservationHistory(Reservation reservation, TargetType targetType, Long targetId,
							  ReservationStatus fromStatus, ReservationStatus toStatus,
							  User changedBy, String reason) {
		this.reservation = reservation;
		this.targetType = targetType;
		this.targetId = targetId;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.changedBy = changedBy;
		this.reason = reason;
	}
}