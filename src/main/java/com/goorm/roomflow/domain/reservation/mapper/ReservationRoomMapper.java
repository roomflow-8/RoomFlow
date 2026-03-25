package com.goorm.roomflow.domain.reservation.mapper;

import com.goorm.roomflow.domain.reservation.dto.response.EquipmentItem;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservationRoomMapper {
    @Mapping(target = "reservationId", source = "reservation.reservationId")
    @Mapping(target = "roomId", source = "meetingRoom.roomId")
    @Mapping(target = "roomName", source = "meetingRoom.roomName")
    @Mapping(target = "capacity", source = "meetingRoom.capacity")
    @Mapping(target = "reservationDate", source = "reservationDate")
    @Mapping(target = "reservationStatus", source = "reservation.status")
    @Mapping(target = "reservationTimeSlots", source = "reservationTimeSlots")
    @Mapping(target = "roomAmount", source = "roomAmount")
    @Mapping(target = "equipments", source = "equipments")
    @Mapping(target = "equipmentAmount", source = "equipmentAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    ReservationRoomRes toReservationRoomRes(
            Reservation reservation,
            MeetingRoom meetingRoom,
            List<ReservationTimeSlot> reservationTimeSlots,
            List<EquipmentItem> equipments,
            LocalDate reservationDate,
            BigDecimal roomAmount,
            BigDecimal equipmentAmount,
            BigDecimal totalAmount
    );
}
