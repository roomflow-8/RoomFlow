package com.goorm.roomflow.domain.user.dto;

import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UserReservationDTO(
        Long reservationId,
        String roomName,
        String roomImageUrl,
        ReservationStatus status,
        BigDecimal totalAmount,
        String memo,
        LocalDateTime cancelledAt,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime slotStartAt,          // 필터링용 (가장 이른 시작 시간)
        LocalDateTime slotEndAt,            // 필터링용 (가장 늦은 종료 시간)
        List<LocalDateTime> slotStartTimes, // 화면 표시용 시작 시간 목록
        List<LocalDateTime> slotEndTimes,   // 화면 표시용 종료 시간 목록
        List<UserEquipmentItem> equipments,
        List<CancelledEquipmentGroup> cancelledEquipmentGroups, // 취소 이벤트별로 그룹핑된 비품 이력
        BigDecimal cancelledEquipmentTotalAmount, // cancelled 상태의 비품 전체 합산 금액
        BigDecimal equipmentTotalAmount,
        BigDecimal roomHourlyPrice,         // 시간당 회의실 가격 (금액 분리 표시용)
        int totalSlotHours,                 // 총 예약 시간
        BigDecimal grandTotalAmount         // 회의실 + 비품 합산 최종 금액
) {}
