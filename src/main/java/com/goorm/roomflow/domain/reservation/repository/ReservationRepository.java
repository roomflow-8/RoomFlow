package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
        select r
        from Reservation r
        left join fetch r.reservationRooms rr
        left join fetch rr.roomSlot rs
        where r.status = :status
          and r.createdAt <= :threshold
    """)
    List<Reservation> findExpiredPendingReservations(
            @Param("status") ReservationStatus status,
            @Param("threshold") LocalDateTime threshold
    );

    @Query("select r from Reservation r join fetch r.meetingRoom where r.user.userId = :userId order by r.createdAt desc")
    List<Reservation> findByUserUserIdWithRoom(@Param("userId") Long userId);

    @Query("""
        select r
        from Reservation r
        join fetch r.user u
        join fetch r.meetingRoom mr
        where r.reservationId = :reservationId
    """)
    Optional<Reservation> findByIdWithUserAndMeetingRoom(@Param("reservationId") Long reservationId);
    // reservation - meetingRoom - reservationRooms - roomSlot 한 번에 fetch join
    @Query("""
            select r from Reservation r
                     join fetch r.meetingRoom mr
                     left join fetch r.reservationRooms rr
                     left join fetch rr.roomSlot
                     where r.user.userId = :userId order by r.createdAt desc
            """)
    List<Reservation> findReservationByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<Reservation> findByReservationId(Long reservationId);


    @Query("""
        select distinct r
        from Reservation r
        join fetch r.user u
        join fetch r.reservationRooms rr
        join fetch rr.roomSlot rs
        join fetch r.meetingRoom mr
        where r.reservationId = :reservationId
    """)
    Optional<Reservation> findByIdWithAll(Long reservationId);
}
