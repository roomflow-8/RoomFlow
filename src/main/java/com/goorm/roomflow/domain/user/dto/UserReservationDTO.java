package com.goorm.roomflow.domain.user.dto;

import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserReservationDTO {

    private Long reservationId;
    private String roomName;
    private String roomImageUrl;
    private ReservationStatus status;
    private BigDecimal totalAmount;
    private String memo;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime slotStartAt;          // 필터링용 (가장 이른 시작 시간)
    private LocalDateTime slotEndAt;            // 필터링용 (가장 늦은 종료 시간)
    private List<LocalDateTime> slotStartTimes; // 화면 표시용 시작 시간 목록
    private List<LocalDateTime> slotEndTimes;   // 화면 표시용 종료 시간 목록
    private List<EquipmentItem> equipments;     // 예약된 비품 목록
    private BigDecimal equipmentTotalAmount;    // 비품 대여료 합계
    private BigDecimal roomHourlyPrice;         // 시간당 회의실 가격 (금액 분리 표시용)
    private int totalSlotHours;                 // 총 예약 시간 (시간 단위)
    private BigDecimal grandTotalAmount;        // 회의실 + 비품 합산 최종 금액

    @Getter
    @Builder
    public static class EquipmentItem {
        private String equipmentName;
        private int quantity;
        private BigDecimal totalAmount;
    }
}
