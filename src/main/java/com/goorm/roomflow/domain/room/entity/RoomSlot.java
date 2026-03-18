package com.goorm.roomflow.domain.room.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
		name = "room_slots",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_room_slot",
						columnNames = {"room_id", "slot_start_at", "slot_end_at"}
				)
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 자동 날짜 매핑을 위해 필수
public class RoomSlot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long roomSlotId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private MeetingRoom meetingRoom;

	@Column(nullable = false)
	private LocalDateTime slotStartAt;

	@Column(nullable = false)
	private LocalDateTime slotEndAt;

	@Column(nullable = false)
	private boolean isActive = true;

	// --- 시간 관련 필드 직접 삽입 ---
	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public RoomSlot(MeetingRoom meetingRoom, LocalDateTime slotStartAt, LocalDateTime slotEndAt, boolean isActive) {
		// 시간 선후 관계 검증 (SQL의 CHECK 제약조건 로직)
		if (slotStartAt != null && slotEndAt != null && !slotStartAt.isBefore(slotEndAt)) {
			throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 합니다.");
		}

		this.meetingRoom = meetingRoom;
		this.slotStartAt = slotStartAt;
		this.slotEndAt = slotEndAt;
		this.isActive = isActive;
	}

	// 비즈니스 로직
	public void updateActiveStatus(boolean isActive) {
		this.isActive = isActive;
	}
}