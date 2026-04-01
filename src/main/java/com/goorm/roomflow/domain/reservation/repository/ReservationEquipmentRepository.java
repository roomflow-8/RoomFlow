package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationEquipmentRepository extends JpaRepository<ReservationEquipment, Long> {


	/**
	 * 특정 시간대에 예약된 비품 수량 계산 (CANCELLED, EXPIRED 제외)
	 * - Service Layer에서 재고 검증 시 사용
	 * <p>
	 * 수정 전:
	 * AND re.status NOT IN ('CANCELLED', 'EXPIRED')
	 * ->
	 * 수정 후:
	 * AND re.status IN ('PENDING', 'CONFIRMED')
	 */
	@Query("""
			    SELECT COALESCE(SUM(re.quantity), 0)
			    FROM ReservationEquipment re
			    JOIN re.reservation r
			    JOIN ReservationRoom rr ON rr.reservation = r
			    JOIN rr.roomSlot rs
			    WHERE re.equipment.equipmentId = :equipmentId
			    AND rs.slotStartAt < :endTime
			    AND rs.slotEndAt > :startTime
				AND r.status NOT IN ('CANCELLED', 'EXPIRED')
				AND (re.status = 'CONFIRMED'
              			OR (re.status = 'PENDING' AND r.reservationId != :reservationId)
			    )
			""")
	int calculateReservedQuantity(
			@Param("reservationId") Long reservationId,
			@Param("equipmentId") Long equipmentId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime
	);

	/**
	 * 예약 ID로 비품 목록 조회 (특정 상태 제외)
	 */
	List<ReservationEquipment> findByReservation_ReservationIdAndStatusNot(
			Long reservationId,
			ReservationStatus status
	);

	@EntityGraph(attributePaths = {"equipment"})
	List<ReservationEquipment> findByReservation_ReservationIdAndStatus(
			Long reservationId,
			ReservationStatus status
	);

	/**
	 * 예약 ID로 비품 목록 조회
	 */
	List<ReservationEquipment> findByReservation_ReservationId(Long reservationId);

	// 여러 reservationId를 IN 조건으로 한 번에 조회 + equipment fetch join
	@Query("""
		SELECT re
		FROM ReservationEquipment re
		JOIN FETCH re.equipment e
		WHERE re.reservation.reservationId IN :reservationIds
	""")
	List<ReservationEquipment> findAllByReservationIdsWithEquipment(@Param("reservationIds") List<Long> reservationIds);

}
