package com.goorm.roomflow.domain.reservation.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
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
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reservationId;

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "user_id", nullable = false)
//	private User user;
	@Column(name = "user_id", nullable = false)
	private Long userId;

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

	@Builder
	public Reservation(
//			User user,
					   Long userId,
					   MeetingRoom meetingRoom, String idempotencyKey,
					   BigDecimal totalAmount) {

//		this.user = user;
		this.userId = userId;
		this.meetingRoom = meetingRoom;
		this.idempotencyKey = idempotencyKey;
		this.totalAmount = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
		this.status = ReservationStatus.PENDING;
	}

	/*
	260319 ES getReservationInfoTest 로 추가
	 */
	public Reservation(ReservationStatus reservationStatus, MeetingRoom room) {
		super();
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

	public void updateTotalAmount(BigDecimal newTotalAmount) {
		this.totalAmount = newTotalAmount;
	}

}
