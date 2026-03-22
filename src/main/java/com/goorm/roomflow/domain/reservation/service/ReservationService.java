package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.reservation.dto.request.AddEquipmentsReq;
import com.goorm.roomflow.domain.reservation.dto.request.CancelReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;

import java.util.List;

public interface ReservationService {
    ReservationRoomRes readReservationRoom(Long reservationId);
    ReservationRoomRes createReservationRoom(CreateReservationRoomReq request) ;

    //비품 관련 메서드
    List<ReservationEquipment> addEquipmentsToReservation(Long reservationId, AddEquipmentsReq request);
    List<EquipmentAvailabilityDto> getAvailableEquipments(Long reservationId);

    ReservationRoomRes createReservationRoom(CreateReservationRoomReq request);
    void confirmReservation(Long reservationId, ConfirmReservationReq request);
    void cancelReservation(Long reservationId, CancelReservationReq request);
}
