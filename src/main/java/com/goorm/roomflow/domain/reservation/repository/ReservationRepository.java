package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
        select r
        from Reservation r
        where r.status = :status
          and r.createdAt <= :threshold
    """)
    List<Reservation> findExpiredPendingReservations(
            @Param("status") ReservationStatus status,
            @Param("threshold") LocalDateTime threshold
    );

    @Query("select r from Reservation r join fetch r.meetingRoom where r.user.userId = :userId order by r.createdAt desc")
    List<Reservation> findByUserUserIdWithRoom(@Param("userId") Long userId);
}
