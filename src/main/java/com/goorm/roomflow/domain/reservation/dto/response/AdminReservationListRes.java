package com.goorm.roomflow.domain.reservation.dto.response;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * 관리자 예약 목록 조회용 Response
 */
public record AdminReservationListRes(
		Long reservationId,
		String userName,           // 회원명
		String userEmail,          // 회원 이메일 (username 대신)
		String roomName,           // 회의실명
		LocalDateTime startAt,     // 시작 시간
		LocalDateTime endAt,       // 종료 시간
		Integer equipmentCount,    // 비품 개수
		BigDecimal totalAmount,    // 총 금액
		ReservationStatus status,  // 상태
		LocalDateTime createdAt    // 예약 생성일
) {

	public String getFormattedStartAt() {
		if (startAt != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
			return startAt.format(formatter);
		}
		return "";
	}

	public String getFormattedEndAt() {
		if (endAt != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
			return endAt.format(formatter);
		}
		return "";
	}

	public String getDate() {
		if (startAt != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			return startAt.format(formatter);
		}
		return "";
	}

	public String getTimeRange() {
		if (startAt != null && endAt != null) {
			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
			return startAt.format(timeFormatter) + "-" + endAt.format(timeFormatter);
		}
		return "";
	}

	public String getStatusBadgeClass() {
		if (status == null) return "status-pending";

		return switch (status) {
			case PENDING -> "status-pending";
			case CONFIRMED -> "status-confirmed";
			case CANCELLED -> "status-cancelled";
			case EXPIRED -> "status-expired";
			case NONE -> "status-none";
		};
	}

	public String getFormattedTotalAmount() {
		if (totalAmount != null) {
			return String.format("%,d원", totalAmount.intValue());
		}
		return "0원";
	}

	public boolean hasEquipment() {
		return equipmentCount != null && equipmentCount > 0;
	}

	// 정적 팩토리 메서드
	public static AdminReservationListRes from(Reservation reservation) {
		LocalDateTime startAt = null;
		LocalDateTime endAt = null;
		String roomName = "";

		// reservationRooms가 비어있지 않은 경우
		if (reservation.getReservationRooms() != null && !reservation.getReservationRooms().isEmpty()) {
			// 가장 빠른 시작 시간 찾기
			ReservationRoom firstRoom = reservation.getReservationRooms().stream()
					.min(Comparator.comparing(rr -> rr.getRoomSlot().getSlotStartAt()))
					.orElse(null);

			// 가장 늦은 종료 시간 찾기
			ReservationRoom lastRoom = reservation.getReservationRooms().stream()
					.max(Comparator.comparing(rr -> rr.getRoomSlot().getSlotEndAt()))
					.orElse(null);

			if (firstRoom != null) {
				startAt = firstRoom.getRoomSlot().getSlotStartAt();
				roomName = firstRoom.getMeetingRoom().getRoomName();
			}

			if (lastRoom != null) {
				endAt = lastRoom.getRoomSlot().getSlotEndAt();
			}
		}

		return new AdminReservationListRes(
				reservation.getReservationId(),
				reservation.getUser() != null ? reservation.getUser().getName() : "알 수 없음",
				reservation.getUser() != null ? reservation.getUser().getEmail() : "",  
				roomName,
				startAt,
				endAt,
				reservation.getReservationEquipments() != null ? reservation.getReservationEquipments().size() : 0,
				reservation.getTotalAmount(),
				reservation.getStatus(),
				reservation.getCreatedAt()
		);
	}

	// Builder
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Long reservationId;
		private String userName;
		private String userEmail; 
		private String roomName;
		private LocalDateTime startAt;
		private LocalDateTime endAt;
		private Integer equipmentCount;
		private BigDecimal totalAmount;
		private ReservationStatus status;
		private LocalDateTime createdAt;

		public Builder reservationId(Long reservationId) {
			this.reservationId = reservationId;
			return this;
		}

		public Builder userName(String userName) {
			this.userName = userName;
			return this;
		}

		public Builder userEmail(String userEmail) {
			this.userEmail = userEmail;
			return this;
		}

		public Builder roomName(String roomName) {
			this.roomName = roomName;
			return this;
		}

		public Builder startAt(LocalDateTime startAt) {
			this.startAt = startAt;
			return this;
		}

		public Builder endAt(LocalDateTime endAt) {
			this.endAt = endAt;
			return this;
		}

		public Builder equipmentCount(Integer equipmentCount) {
			this.equipmentCount = equipmentCount;
			return this;
		}

		public Builder totalAmount(BigDecimal totalAmount) {
			this.totalAmount = totalAmount;
			return this;
		}

		public Builder status(ReservationStatus status) {
			this.status = status;
			return this;
		}

		public Builder createdAt(LocalDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		public AdminReservationListRes build() {
			return new AdminReservationListRes(
					reservationId, userName, userEmail, roomName,  
					startAt, endAt, equipmentCount, totalAmount,
					status, createdAt
			);
		}
	}
}