package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.CancelReservationReq;
import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationListRes;
import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationStatisticsRes;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReservationServiceImpl implements AdminReservationService {

	private final ReservationRepository reservationRepository;
	private final ReservationService reservationService;


	@Override
	public AdminReservationStatisticsRes getStatistics() {
		log.debug("예약 통계 조회 시작");

		try {
			// 전체 예약 수
			Long total = reservationRepository.count();
			log.debug("전체 예약 수: {}", total);

			// 확정된 예약 수
			Long confirmed = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
			log.debug("확정된 예약 수: {}", confirmed);

			// 취소된 예약 수
			Long cancelled = reservationRepository.countByStatus(ReservationStatus.CANCELLED);
			log.debug("취소된 예약 수: {}", cancelled);

			// 오늘 예약 수
			LocalDate todayDate = LocalDate.now();
			LocalDateTime start = todayDate.atStartOfDay();
			LocalDateTime end = todayDate.plusDays(1).atStartOfDay();

			Long today = reservationRepository.countByDate(start, end);

			log.debug("오늘 예약 수: {}", today);

			AdminReservationStatisticsRes statistics = AdminReservationStatisticsRes.of(
					total,
					confirmed,
					cancelled,
					today
			);

			log.info("예약 통계 조회 완료: {}", statistics);
			return statistics;

		} catch (Exception e) {
			log.error("예약 통계 조회 중 오류 발생", e);
			// 오류 발생 시 모두 0으로 반환
			return AdminReservationStatisticsRes.of(0L, 0L, 0L, 0L);
		}
	}


	@Override
	public Page<AdminReservationListRes> getReservationsByTab(
			String tab, String searchQuery, String status, String roomName,
			String startDate, String endDate, Pageable pageable) {

		log.info("예약 목록 조회 - tab: {}, search: {}, status: {}, room: {}, start: {}, end: {}",
				tab, searchQuery, status, roomName, startDate, endDate);

		try {
			// 파라미터 변환
			LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
			LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
			ReservationStatus reservationStatus = status != null && !status.isEmpty() && !"all".equals(status)
					? ReservationStatus.valueOf(status.toUpperCase()) : null;

			log.debug("변환된 값 - reservationStatus: {}, start: {}, end: {}",
					reservationStatus, start, end);


			// 전체 데이터 조회
			Page<Reservation> reservations = reservationRepository.searchReservations(
					searchQuery, reservationStatus, roomName, start, end, Pageable.unpaged()
			);


			// 탭별 필터링 (애플리케이션 레벨)
			List<Reservation> filteredList = filterByTab(reservations.getContent(), tab);

			// DTO 변환
			List<AdminReservationListRes> dtoList = filteredList.stream()
					.map(AdminReservationListRes::from)
					.collect(Collectors.toList());

			log.info("예약 목록 조회 완료 - 총 {}건", dtoList.size());

			// 전체 필터링된 데이터 개수
			int totalElements = dtoList.size();

			log.info("필터링 후 전체 데이터 - 총 {}건", totalElements);

			// 페이징 처리
			int pageSize = pageable.getPageSize();
			int currentPage = pageable.getPageNumber();
			int startIdx = currentPage * pageSize;
			int endIdx = Math.min(startIdx + pageSize, totalElements);

			// offset이 리스트 크기를 넘으면 빈 페이지 반환
			if (startIdx >= totalElements && totalElements > 0) {
				return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
			}

			// 빈 리스트 처리
			if (totalElements == 0) {
				return new PageImpl<>(Collections.emptyList(), pageable, 0);
			}

			List<AdminReservationListRes> pagedList = dtoList.subList(startIdx, endIdx);

			log.debug("페이징 완료 - {}페이지, 페이지당 {}건, 전체 {}건 중 {}건 표시",
					currentPage + 1, pageSize, totalElements, pagedList.size());


			Page<AdminReservationListRes> result = new PageImpl<>(pagedList, pageable, totalElements);

			log.debug("Total Pages: {}", result.getTotalPages());
			log.debug("Total Elements: {}", result.getTotalElements());

			return result;

		} catch (Exception e) {
			log.error("예약 목록 조회 실패", e);
			return Page.empty(pageable);
		}
	}


	@Override
	public List<String> getAllRoomNames() {
		try {
			List<String> roomNames = reservationRepository.findAllDistinctRoomNames();
			log.info("회의실 목록 조회 완료 - {}개", roomNames.size());
			return roomNames;
		} catch (Exception e) {
			log.error("회의실 목록 조회 실패", e);
			return List.of();
		}
	}

	// 탭별 필터링 (애플리케이션 레벨)
	private List<Reservation> filterByTab(List<Reservation> reservations, String tab) {
		if (tab == null || "all".equals(tab)) {
			return reservations;
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDate today = LocalDate.now();

		return reservations.stream()
				.filter(r -> {
					// 시작 시간 가져오기
					LocalDateTime startAt = r.getReservationRooms().isEmpty() ? null
							: r.getReservationRooms().getFirst().getRoomSlot().getSlotStartAt();

					if (startAt == null) return false;

					return switch (tab) {
						case "today" -> startAt.toLocalDate().equals(today);
						case "upcoming" -> startAt.isAfter(now) && r.getStatus() != ReservationStatus.CANCELLED;
						case "past" -> startAt.isBefore(now);
						default -> true;
					};
				})
				.collect(Collectors.toList());
	}

	@Override
	public void cancelReservationByAdmin(Long adminId, Long reservationId, String cancelReason) {
		log.info("관리자에 의한 예약 취소 시작 - adminId: {}, reservationId: {}", adminId, reservationId);

		// 예약 정보 조회
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		// 이미 취소된 예약인지 확인
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw new BusinessException(ErrorCode.RESERVATION_CANCELLED);
		}


		// 취소 요청 객체 생성 (record 타입)
		CancelReservationReq request = new CancelReservationReq(
				cancelReason != null ? cancelReason : "관리자에 의한 취소",
				null
		);

		// 기존 예약 취소 서비스 재사용
		reservationService.cancelReservation(adminId, reservationId, request);

		log.info("관리자에 의한 예약 취소 완료 - adminId: {}, reservationId: {}", adminId, reservationId);

	}


}
