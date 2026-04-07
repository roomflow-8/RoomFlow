package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.reservation.dto.request.*;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.user.service.CustomUser;

import java.util.List;

public interface ReservationService {

    Reservation getReservation(Long userId, Long reservationId);

    ReservationRoomRes readReservationRoom(CustomUser user, Long reservationId);
    ReservationRoomRes createReservationRoomTransactional(Long userId, CreateReservationRoomReq request) ;

    //비품 관련 메서드
    EquipmentReservationRes addEquipmentsToReservation(Long userId, Long reservationId, AddEquipmentsReq request);
    List<EquipmentAvailabilityDto> getAvailableEquipments(Long userId, Long reservationId);

    void confirmReservation(Long userId, Long reservationId, ConfirmReservationReq request);
    void confirmEquipmentsService(Long userId, Long reservationId, List<Long> reservationEquipmentIds);

    void cancelReservation(Long userId, Long reservationId, CancelReservationReq request);
    void expireReservation(Long userId, Long reservationId);
    void cancelReservationEquipments(Long userId, Long reservationId, CancelReservationEquipmentsReq request);

    void expirePendingEquipments(Long changedByUserId, List<Long> reservationEquipmentIds);

    ReservationRoomRes readConfirmedReservationRoom(CustomUser user, Long reservationId);
}
