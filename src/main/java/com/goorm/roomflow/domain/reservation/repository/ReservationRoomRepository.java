package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {
    @EntityGraph(attributePaths = {"roomSlot"})
    List<ReservationRoom> findByReservation(Reservation reservation);

    @Query("""
        select count(rr) > 0
        from ReservationRoom rr
        join rr.roomSlot rs
        join rr.reservation r
        where rs.meetingRoom.roomId = :roomId
          and rs.slotStartAt >= :now
          and r.status in ('PENDING', 'CONFIRMED')
    """)
    boolean existsFutureReservationByRoomId(
            Long roomId,
            LocalDateTime now
    );
}
