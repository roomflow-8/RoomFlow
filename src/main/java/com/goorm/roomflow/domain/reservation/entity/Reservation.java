package com.goorm.roomflow.domain.reservation.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "user_id")
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

	@OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY)
	private List<ReservationRoom> reservationRooms;

	@OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY)
	private List<ReservationEquipment> reservationEquipments;

	@Builder
	public Reservation(
			User user,
			MeetingRoom meetingRoom, String idempotencyKey,
			BigDecimal totalAmount) {

		this.user = user;
		this.meetingRoom = meetingRoom;
		this.idempotencyKey = idempotencyKey;
		this.totalAmount = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
		this.status = ReservationStatus.PENDING;
	}

	//미사용시 삭제예정
	public Reservation(ReservationStatus reservationStatus, MeetingRoom room) {
		super();
	}

	public void confirm(String memo) {

		if (this.status != ReservationStatus.PENDING) {

			if (this.status == ReservationStatus.CONFIRMED) {
				throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CONFIRMED);
			}

			if (this.status == ReservationStatus.EXPIRED) {
				throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
			}

			throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATUS);
		}

		this.status = ReservationStatus.CONFIRMED;
		this.memo = memo;
	}

	public void cancel(String reason) {

		if(this.status != ReservationStatus.CONFIRMED) {

			if(this.status == ReservationStatus.CANCELLED) {
				throw new BusinessException(ErrorCode.RESERVATION_CANCELLED);
			}

			throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATUS);
		}

		this.status = ReservationStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
		this.cancelReason = reason;
	}

	public void expire(String reason) {
		this.status = ReservationStatus.EXPIRED;
		this.cancelledAt = LocalDateTime.now();
		this.cancelReason = reason;
	}

	public boolean isPendingTimeout(LocalDateTime now, long pendingMinutes) {
		return this.status == ReservationStatus.PENDING
				&& this.getCreatedAt().plusMinutes(pendingMinutes).isBefore(now);
	}

	public void updateTotalAmount(BigDecimal newTotalAmount) {
		this.totalAmount = newTotalAmount;
	}

}
