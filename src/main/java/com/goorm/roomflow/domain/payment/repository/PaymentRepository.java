package com.goorm.roomflow.domain.payment.repository;

import com.goorm.roomflow.domain.payment.entity.Payment;
import com.goorm.roomflow.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, Long> {
	Optional<Payment> findByOrderId(String orderId);

	Optional<Payment> findByPaymentKey(String paymentKey);

	Optional<Payment> findByReservation_ReservationId(Long reservationId);

	Optional<Payment> findByReservation_ReservationIdAndStatusIn(
			Long reservationId, List<PaymentStatus> statuses);

	boolean existsByOrderId(String orderId);

	@EntityGraph(attributePaths = {"user"})
	List<Payment> findAllByStatusOrderByApprovedAtDesc(
			PaymentStatus status
	);

	long countByStatus(PaymentStatus status);
}
