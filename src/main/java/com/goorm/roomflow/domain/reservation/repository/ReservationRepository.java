package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
