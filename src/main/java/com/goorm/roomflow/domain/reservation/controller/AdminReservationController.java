package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationListRes;
import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationStatisticsRes;
import com.goorm.roomflow.domain.reservation.service.AdminReservationService;
import com.goorm.roomflow.domain.user.service.CustomUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

	private final AdminReservationService adminReservationService;

	@GetMapping
	public String reservationManagementList(
			@RequestParam(required = false, defaultValue = "all") String tab,
			@RequestParam(required = false) String searchQuery,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String room,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
			Model model) {


		log.debug("예약 관리 페이지 진입 - tab: {}, searchQuery: {}, status: {}, room: {}, startDate: {}, endDate: {}",
				tab, searchQuery, status, room, startDate, endDate);

		try {
			// 통계 데이터 조회
			AdminReservationStatisticsRes stats = adminReservationService.getStatistics();
			model.addAttribute("stats", stats);


			// 예약 목록 조회 (탭별 필터링 + 검색 조건)
			Page<AdminReservationListRes> reservations = adminReservationService.getReservationsByTab(
					tab, searchQuery, status, room, startDate, endDate, pageable
			);

			model.addAttribute("reservations", reservations);

			// 검색 조건 유지
			model.addAttribute("tab", tab);
			model.addAttribute("searchQuery", searchQuery);
			model.addAttribute("status", status);
			model.addAttribute("room", room);
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);

			// 회의실 목록 (드롭다운용)
			List<String> rooms = adminReservationService.getAllRoomNames();
			model.addAttribute("rooms", rooms);

			log.info("예약 목록 조회 완료 - 총 {}건", reservations.getTotalElements());

			return "admin/reservation/reservation-list";

		} catch (Exception e) {
			log.error("예약 관리 페이지 로딩 중 오류", e);
			// 오류 발생 시 빈 통계 전달
			model.addAttribute("stats", AdminReservationStatisticsRes.of(0L, 0L, 0L, 0L));
			model.addAttribute("reservations", Page.empty());
			return "admin/reservation/reservation-list";
		}

	}


	/**
	 * 관리자 - 예약 강제 취소
	 */
	@PostMapping("/{reservationId}/cancel")
	public String adminCancelReservation(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId,
			@RequestParam(required = false) String cancelReason,
			RedirectAttributes redirectAttributes
	) {

		log.info("관리자 예약 취소 - reservationId: {}, adminId: {}",
				reservationId, currentUser.getUserId());

		try {

			// 관리자용 취소 서비스 호출
			adminReservationService.cancelReservationByAdmin(  currentUser.getUserId(), reservationId, cancelReason );

			redirectAttributes.addFlashAttribute("alertType", "success");
			redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");

		} catch (IllegalStateException e) {
			log.warn("예약 취소 실패: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("alertType", "error");
			redirectAttributes.addFlashAttribute("message", e.getMessage());
			return "redirect:/common/error";
		} catch (Exception e) {
			log.error("예약 취소 중 예외 발생: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("alertType", "error");
			redirectAttributes.addFlashAttribute("message", "예약 취소 중 오류가 발생했습니다.");
			return "redirect:/common/error";
		}
		return "redirect:/admin/reservations";
	}



/*
2.

	// 예약 상세보기
	@GetMapping("/{reservationId}")
	public String reservationDetail(@PathVariable Long reservationId, Model model) {
		AdminReservationListRes reservation = reservationService.getReservationById(reservationId);
		model.addAttribute("reservation", reservation);
		return "admin/reservation/reservation-detail";
	}

	// 예약 히스토리 페이지
	@GetMapping("/history")
	public String reservationHistory(
			@PageableDefault(size = 20, sort = "modifiedAt", direction = Sort.Direction.DESC) Pageable pageable,
			Model model) {

		Page<ReservationHistoryDTO> history = reservationService.getReservationHistory(pageable);
		model.addAttribute("history", history);

		return "admin/reservations/reservation-history";
	}




*/


}
