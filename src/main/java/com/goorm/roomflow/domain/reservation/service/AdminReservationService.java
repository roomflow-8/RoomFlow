package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationListRes;
import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationStatisticsRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminReservationService {

	/**
	 * 예약 통계 조회
	 * @return 전체, 확정, 취소, 오늘 예약 수
	 */
	AdminReservationStatisticsRes getStatistics();

	/**
	 * 탭별 예약 조회 (검색 + 필터링)
	 */
	Page<AdminReservationListRes> getReservationsByTab(
			String tab,
			String searchQuery,
			String status,
			String roomName,
			String startDate,
			String endDate,
			Pageable pageable
	);

	/**
	 * 모든 회의실 이름 조회
	 */
	List<String> getAllRoomNames();

	/**
	 * 관리자에 의한 예약 취소
	 * @param reservationId 취소할 예약 ID
	 * @param cancelReason 취소 사유
	 */
	void cancelReservationByAdmin(Long adminId, Long reservationId, String cancelReason);


}
