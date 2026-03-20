package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {
    List<ReservationRoom> findByReservation(Reservation reservation);
}
