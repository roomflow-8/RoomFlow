package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.reservation.dto.request.*;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.entity.Reservation;

import java.util.List;

public interface ReservationService {

    Reservation getReservation(Long reservationId);

    ReservationRoomRes readReservationRoom(Long userId, Long reservationId);
    ReservationRoomRes createReservationRoomTransactional(Long userId, CreateReservationRoomReq request) ;

    //비품 관련 메서드
    EquipmentReservationRes addEquipmentsToReservation(Long reservationId, AddEquipmentsReq request);
    List<EquipmentAvailabilityDto> getAvailableEquipments(Long reservationId);

    void confirmReservation(Long userId, Long reservationId, ConfirmReservationReq request);
    void confirmEquipmentsService(Long reservationId, List<Long> reservationEquipmentIds);

    void cancelReservation(Long userId, Long reservationId, CancelReservationReq request);
    void expireReservation(Long userId, Long reservationId);
    void cancelReservationEquipments(Long reservationId, CancelReservationEquipmentsReq request);

    void expirePendingEquipments(List<Long> reservationEquipmentIds, Long changedByUserId);
}
