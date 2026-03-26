package com.goorm.roomflow.domain.room.entity;

import com.goorm.roomflow.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "meeting_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRoom extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long roomId;

	@Column(nullable = false, length = 50)
	private String roomName;

	@Column(nullable = false)
	private int capacity;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal hourlyPrice;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoomStatus status = RoomStatus.AVAILABLE;

	@Column(nullable = false)
	private int totalReservations = 0;

	@Column(length = 1000)
	private String imageUrl;

	@Builder
	public MeetingRoom(String roomName, int capacity, String description,
					   BigDecimal hourlyPrice, RoomStatus status, String imageUrl) {

		this.roomName = roomName;
		this.capacity = capacity;
		this.description = description;
		this.hourlyPrice = (hourlyPrice != null) ? hourlyPrice : BigDecimal.ZERO;
		this.status = (status != null) ? status : RoomStatus.AVAILABLE;
		this.imageUrl = imageUrl;
		this.totalReservations = 0;
	}

	// 회의실 정보 수정
	public void updateRoomInfo(String roomName, int capacity, String description) {
		validateRoomName(roomName);
		validateCapacity(capacity);
		validateHourlyPrice(hourlyPrice);

		this.roomName = roomName;
		this.capacity = capacity;
		this.description = description;
	}

	public void updateStatus(RoomStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("회의실 상태는 필수입니다.");
		}
		this.status = status;
	}

	public void incrementReservations() {
		this.totalReservations++;
	}
	public void decrementReservations() {
		this.totalReservations--;
	}

	private void validateRoomName(String roomName) {
		if (roomName == null || roomName.isBlank()) {
			throw new IllegalArgumentException("회의실 이름은 필수입니다.");
		}
	}

	private void validateCapacity(int capacity) {
		if (capacity < 1) {
			throw new IllegalArgumentException("회의실 정원은 1 이상이어야 합니다.");
		}
	}

	private void validateHourlyPrice(BigDecimal hourlyPrice) {
		if (hourlyPrice != null && hourlyPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("회의실 요금은 0 이상이어야 합니다.");
		}
	}

}