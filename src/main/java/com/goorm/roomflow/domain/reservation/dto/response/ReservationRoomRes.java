package com.goorm.roomflow.domain.reservation.dto.response;

import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Builder
public record ReservationRoomRes (
        Long reservationId,
        Long roomId,
        String roomName,
        LocalDate reservationDate,
        int capacity,
        ReservationStatus reservationStatus,
        List<ReservationTimeSlot> reservationTimeSlots,
        BigDecimal roomAmount,
        List<EquipmentItem> equipments,
        BigDecimal equipmentAmount,
        BigDecimal totalAmount
) {
    public String priceText() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(totalAmount) + "원";
    }
}
