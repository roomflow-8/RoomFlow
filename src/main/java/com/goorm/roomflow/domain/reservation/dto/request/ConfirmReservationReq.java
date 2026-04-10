package com.goorm.roomflow.domain.reservation.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record ConfirmReservationReq(
        List<Long> reservationEquipmentIds,
        String memo
){ }
