package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;

public interface ReservationService {
    ReservationRoomRes readReservationRoom(Long reservationId);
    ReservationRoomRes createReservationRoom(CreateReservationRoomReq request);
    void confirmReservation(Long reservationId, ConfirmReservationReq request);
}
