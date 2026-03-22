package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.request.AddEquipmentsReq;
import com.goorm.roomflow.domain.reservation.dto.request.CancelReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;

import java.util.List;

public interface ReservationService {
    ReservationRoomRes readReservationRoom(Long reservationId);
    ReservationRoomRes createReservationRoom(CreateReservationRoomReq request) ;

    //비품 관련 메서드
    EquipmentReservationRes addEquipmentsToReservation(Long reservationId, AddEquipmentsReq request);
    List<EquipmentAvailabilityDto> getAvailableEquipments(Long reservationId);

    void confirmReservation(Long reservationId, ConfirmReservationReq request);
    void confirmEquipmentsService(Long reservationId, List<Long> reservationEquipmentIds);

    void cancelReservation(Long reservationId, CancelReservationReq request);
}
