package com.goorm.roomflow.domain.reservation.mapper;

import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-21T16:02:35+0900",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.4.jar, environment: Java 21.0.10 (Azul Systems, Inc.)"
)
@Component
public class ReservationRoomMapperImpl implements ReservationRoomMapper {

    @Override
    public ReservationRoomRes toReservationRoomRes(Reservation reservation, MeetingRoom meetingRoom, List<ReservationTimeSlot> reservationTimeSlots, LocalDate reservationDate) {
        if ( reservation == null && meetingRoom == null && reservationTimeSlots == null && reservationDate == null ) {
            return null;
        }

        ReservationRoomRes.ReservationRoomResBuilder reservationRoomRes = ReservationRoomRes.builder();

        if ( reservation != null ) {
            reservationRoomRes.reservationId( reservation.getReservationId() );
            reservationRoomRes.totalAmount( reservation.getTotalAmount() );
        }
        if ( meetingRoom != null ) {
            reservationRoomRes.roomId( meetingRoom.getRoomId() );
            reservationRoomRes.roomName( meetingRoom.getRoomName() );
            reservationRoomRes.capacity( meetingRoom.getCapacity() );
        }
        List<ReservationTimeSlot> list = reservationTimeSlots;
        if ( list != null ) {
            reservationRoomRes.reservationTimeSlots( new ArrayList<ReservationTimeSlot>( list ) );
        }
        reservationRoomRes.reservationDate( reservationDate );

        return reservationRoomRes.build();
    }
}
