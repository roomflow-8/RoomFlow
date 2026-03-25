package com.goorm.roomflow.domain.reservation.service;

import java.time.LocalTime;

public interface ReservationPolicyService {

    int getIntValue(String key);
    LocalTime getTimeValue(String key);
}
