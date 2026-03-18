package com.goorm.roomflow.domain.reservation.entity;

import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.user.entity.User;
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
@Table(
		name = "reservations",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_reservations_idempotency", columnNames = {"idempotency_key"})
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reservationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private MeetingRoom meetingRoom;

	@Column(nullable = false, length = 100)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus status = ReservationStatus.PENDING;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(columnDefinition = "TEXT")
	private String memo;

	private LocalDateTime cancelledAt;
	private String cancelReason;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public Reservation(User user, MeetingRoom meetingRoom, String idempotencyKey,
					   BigDecimal totalAmount, LocalDateTime startAt, LocalDateTime endAt, String memo) {

		// 시간 선후 관계 검증 (CHECK 제약조건)
		if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
			throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 합니다.");
		}

		this.user = user;
		this.meetingRoom = meetingRoom;
		this.idempotencyKey = idempotencyKey;
		this.totalAmount = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
		this.memo = memo;
		this.status = ReservationStatus.PENDING;
	}

	// --- 비즈니스 로직 ---
	public void confirm() {
		this.status = ReservationStatus.CONFIRMED;
	}

	public void cancel(String reason) {
		this.status = ReservationStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
		this.cancelReason = reason;
	}

}
