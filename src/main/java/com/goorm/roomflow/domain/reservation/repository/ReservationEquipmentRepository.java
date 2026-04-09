package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.dto.response.ReservationEquipmentRes;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationEquipmentRepository extends JpaRepository<ReservationEquipment, Long> {

	/**
	 * 특정 시간대에 예약된 비품 수량 목록 조회
	 */
	@Query("""
        SELECT re.quantity
        FROM ReservationEquipment re
        JOIN re.reservation r
        JOIN ReservationRoom rr ON rr.reservation = r
        JOIN rr.roomSlot rs
        WHERE re.equipment.equipmentId = :equipmentId
        AND rs.slotStartAt < :endTime
        AND rs.slotEndAt > :startTime
        AND r.status NOT IN ('CANCELLED', 'EXPIRED')
        AND (re.status = 'CONFIRMED'
             OR (re.status = 'PENDING' AND r.reservationId != :reservationId))
        """)
	List<Integer> findReservedQuantities(
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

	/**
	 * 비품 예약 ID 목록으로 조회
	 */
	@Query("""
			SELECT re
			FROM ReservationEquipment re
			JOIN FETCH re.reservation r
			WHERE re.reservationEquipmentId IN :reservationEquipmentIds
			""")
	List<ReservationEquipment> findAllByIdWithReservation(
			@Param("reservationEquipmentIds") List<Long> reservationEquipmentIds
	);

	/**
	 * 만료 대상 비품 예약 조회
	 * PENDING 상태이고 생성 시간이 threshold 이전인 것
	 */
	@Query("""
        SELECT re
        FROM ReservationEquipment re
        JOIN FETCH re.reservation r
        WHERE re.status = :status
          AND re.createdAt <= :threshold
        """)
	List<ReservationEquipment> findExpiredPendingEquipments(
			@Param("status") ReservationStatus status,
			@Param("threshold") LocalDateTime threshold
	);


	@Query("""
		select count(re) > 0
		from ReservationEquipment re
		join re.reservation r
		join r.reservationRooms rr
		join rr.roomSlot rs
		where re.equipment.equipmentId = :equipmentId
		  and r.status in ('PENDING', 'CONFIRMED')
		  and rs.slotStartAt >= :now
	""")
	boolean existsFutureReservationByEquipmentId(Long equipmentId, LocalDateTime now);


/*
	// 여러 예약의 비품 일괄 조회
	@Query("SELECT re FROM ReservationEquipment re " +
			"LEFT JOIN FETCH re.equipment " +
			"WHERE re.reservation.id IN :reservationIds")
	List<ReservationEquipment> findAllByReservationIdsWithEquipment(
			@Param("reservationIds") List<Long> reservationIds);

 */

	// 단일 예약의 비품 조회
	@Query("SELECT re FROM ReservationEquipment re " +
			"LEFT JOIN FETCH re.equipment " +
			"WHERE re.reservation.reservationId = :reservationId")
	List<ReservationEquipmentRes> findByReservationIdWithEquipment(
			@Param("reservationId") Long reservationId);

}
