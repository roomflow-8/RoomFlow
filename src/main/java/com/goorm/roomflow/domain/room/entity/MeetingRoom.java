package com.goorm.roomflow.domain.room.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "meeting_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRoom {

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoomStatus status = RoomStatus.AVAILABLE;

	@Column(nullable = false)
	private int totalReservations = 0;

	@Column(length = 1000)
	private String imageUrl;

	// 빌더 패턴을 사용하여 객체 생성 시점에 필수 값 검증 가능
	@Builder
	public MeetingRoom(String roomName, int capacity, String description,
					   BigDecimal hourlyPrice, RoomStatus status, String imageUrl) {
		this.roomName = roomName;
		this.capacity = capacity;
		this.description = description;
		this.hourlyPrice = (hourlyPrice != null) ? hourlyPrice : BigDecimal.ZERO;
		this.status = (status != null) ? status : RoomStatus.AVAILABLE;
		this.imageUrl = imageUrl;
		this.totalReservations = 0; // 초기값 강제
	}

	// --- 비즈니스 로직 (Setter 대신 도메인 메서드 사용) ---
	public void updateRoomInfo(String roomName, int capacity, String description) {
		this.roomName = roomName;
		this.capacity = capacity;
		this.description = description;
	}

	public void changeStatus(RoomStatus status) {
		this.status = status;
	}

	public void incrementReservations() {
		this.totalReservations++;
	}

}