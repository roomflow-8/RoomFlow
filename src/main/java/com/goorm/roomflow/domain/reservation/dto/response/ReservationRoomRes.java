package com.goorm.roomflow.domain.reservation.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Builder
public record ReservationRoomRes (
        Long reservationId,
        Long roomId,
        String roomName,
        int capacity,
        List<ReservationTimeSlot> reservationTimeSlots,
        BigDecimal totalAmount
) {
    public String priceText() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(totalAmount) + "원";
    }
}
