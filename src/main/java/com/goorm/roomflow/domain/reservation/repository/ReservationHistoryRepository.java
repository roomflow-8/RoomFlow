package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.ReservationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationHistoryRepository extends JpaRepository<ReservationHistory, Long> {
}
