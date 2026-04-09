package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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


	/**
	 * admin 에서 사용하는 메서드
	 * <p>
	 * countByStatus
	 * countByDate
	 * findByIdWithDetails
	 * searchReservations
	 *
	 */

	// 상태별 예약 수 조회
	long countByStatus(ReservationStatus status);

	//select count(distinct r)
	// 특정 날짜의 예약 수 조회
	@Query("""
			SELECT COUNT(r)
			FROM Reservation r
			JOIN r.reservationRooms rr
			JOIN rr.roomSlot rs
			WHERE rs.slotStartAt >= :start
			AND rs.slotStartAt < :end
			""")
	long countByDate(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);


	// 검색 및 필터링 (복합 조건)
	@Query("SELECT DISTINCT r FROM Reservation r " +
			"LEFT JOIN r.user u " +
			"LEFT JOIN r.reservationRooms rr " +
			"LEFT JOIN rr.roomSlot rs " +
			"LEFT JOIN rr.meetingRoom mr " +
			"WHERE (:searchQuery IS NULL OR :searchQuery = '' OR " +
			"       CAST(r.reservationId AS string) LIKE %:searchQuery% OR " +
			"       u.name LIKE %:searchQuery% OR " +
			"       u.email LIKE %:searchQuery% OR " +
			"       mr.roomName LIKE %:searchQuery%) " +
			"AND (:status IS NULL OR r.status = :status) " +
			"AND (:roomName IS NULL OR :roomName = '' OR mr.roomName = :roomName) " +
			"AND (:startDate IS NULL OR (rs IS NOT NULL AND rs.slotStartAt >= :startDate)) " +
			"AND (:endDate IS NULL OR (rs IS NOT NULL AND rs.slotStartAt <= :endDate))" +
			"AND r.status NOT IN (com.goorm.roomflow.domain.reservation.entity.ReservationStatus.EXPIRED) " +
			"ORDER BY r.reservationId DESC")
	Page<Reservation> searchReservations(
			@Param("searchQuery") String searchQuery,
			@Param("status") ReservationStatus status,
			@Param("roomName") String roomName,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			Pageable pageable
	);


	// 모든 회의실 이름 조회 (중복 제거)
	@Query("SELECT DISTINCT mr.roomName FROM ReservationRoom r " +
			"LEFT JOIN r.meetingRoom mr " +
			" ORDER BY mr.roomName")
	List<String> findAllDistinctRoomNames();


	// 상세 조회 (fetch join)
	@Query("SELECT r FROM Reservation r " +
			"LEFT JOIN FETCH r.user u " +
			"LEFT JOIN FETCH r.meetingRoom mr " +
			"WHERE r.reservationId = :reservationId")
	Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

/*
    // 검색 쿼리 (리스트 반환)
    @Query("SELECT r FROM Reservation r " +
            "LEFT JOIN FETCH r.member " +
            "LEFT JOIN FETCH r.meetingRoom " +
            "WHERE (:searchQuery IS NULL OR :searchQuery = '' OR " +
            "       LOWER(r.id) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "       LOWER(r.member.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "       LOWER(r.member.email) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "       LOWER(r.meetingRoom.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "       LOWER(r.purpose) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:room IS NULL OR :room = '' OR r.meetingRoom.name = :room) " +
            "AND (:startDate IS NULL OR r.date >= :startDate) " +
            "AND (:endDate IS NULL OR r.date <= :endDate)")
    List<Reservation> searchReservations(
            @Param("searchQuery") String searchQuery,
            @Param("status") ReservationStatus status,
            @Param("room") String room,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
*/


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

    // 관리자 회원 목록용 - 유저별 전체 예약 건수
    long countByUserUserId(Long userId);

    // 관리자 회원 목록용 - 유저별 상태별 예약 건수 (취소 건수 집계에 사용)
    long countByUserUserIdAndStatus(Long userId, ReservationStatus status);
}
