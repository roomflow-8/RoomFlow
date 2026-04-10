package com.goorm.roomflow.domain.reservation.event;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.entity.TargetType;
import com.goorm.roomflow.domain.user.entity.User;

public record ReservationStatusChangedEvent(
        Reservation reservation,
        TargetType targetType,
        Long targetId,
        ReservationStatus fromStatus,
        ReservationStatus toStatus,
        User changedBy,
        String reason
) {}