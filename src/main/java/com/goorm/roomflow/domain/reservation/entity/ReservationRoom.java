package com.goorm.roomflow.domain.reservation.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
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
@Table(
		name = "reservation_rooms",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_reservation_room_slot", columnNames = {"reservation_id", "room_slot_id"})
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReservationRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reservationRoomId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private MeetingRoom meetingRoom;

	@OneToOne(fetch = FetchType.LAZY) // 하나의 슬롯은 하나의 예약 정보에만 귀속됨
	@JoinColumn(name = "room_slot_id", nullable = false)
	private RoomSlot roomSlot;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal amount = BigDecimal.ZERO;

	@Builder
	public ReservationRoom(Reservation reservation, MeetingRoom meetingRoom, RoomSlot roomSlot, BigDecimal amount) {
		this.reservation = reservation;
		this.meetingRoom = meetingRoom;
		this.roomSlot = roomSlot;
		this.amount = (amount != null) ? amount : BigDecimal.ZERO;
	}
}
