package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.payment.dto.request.PaymentCheckoutReq;
import com.goorm.roomflow.domain.payment.service.PaymentService;
import com.goorm.roomflow.domain.reservation.dto.request.*;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.service.ReservationLockFacade;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import com.goorm.roomflow.domain.user.service.CustomUser;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

	private final ReservationService reservationService;
	private final ReservationLockFacade reservationLockFacade;
	private final PaymentService paymentService;

	/**
	 * 회의실 예약 생성 처리
	 */
	@PostMapping
	public String createRoomReservation(
			@AuthenticationPrincipal CustomUser currentUser,
			@ModelAttribute CreateReservationRoomReq request,
			RedirectAttributes redirectAttributes) {

		try {
			Long reservationId = reservationLockFacade.createReservationRoom(currentUser.getUserId(), request).reservationId();

			return "redirect:/reservations/rooms/" + reservationId;
		} catch (BusinessException e) {
			redirectAttributes.addFlashAttribute("alertType", "error");
			redirectAttributes.addFlashAttribute("message", e.
					getMessage());

			return "redirect:/rooms";
		}

	}


	/**
	 * 회의실 예약 확인 페이지 조회
	 */
	@GetMapping("/rooms/{reservationId}")
	public String reservationPage(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable("reservationId") Long reservationId,
			Model model) {

		ReservationRoomRes reservationRoomRes = reservationService.readReservationRoom(currentUser, reservationId);

		model.addAttribute("reservationRoom", reservationRoomRes);
		model.addAttribute("hasEquipments", reservationRoomRes.equipments() != null && !reservationRoomRes.equipments().isEmpty());
		return "reservation/confirm";
	}

	/**
	 * 비품 선택 페이지
	 *
	 * @param reservationId 예약 ID
	 * @param model         모델
	 * @return 비품 선택 페이지
	 */
	@GetMapping("/{reservationId}/equipments")
	public String readAvailableEquipments(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId,
			Model model) {

		try {

			// 1. 예약 정보 조회
			Reservation reservation = reservationService.getReservation(currentUser.getUserId(), reservationId);

			// 2. 사용 가능한 비품 목록 조회
			List<EquipmentAvailabilityDto> equipments =
					reservationService.getAvailableEquipments(currentUser.getUserId(), reservationId);

			log.info("비품 선택 페이지 요청 - reservationId: {}, userId: {}", reservationId, currentUser.getUserId());
			log.info("비품 목록 조회 완료 - {} 개", equipments.size());

			model.addAttribute("reservation", reservation);
			model.addAttribute("reservationId", reservationId);
			model.addAttribute("reservationStatus", reservation.getStatus());
			model.addAttribute("equipments", equipments);
			return "equipment/list";

		} catch (Exception e) {
			log.error("비품 목록 조회 실패: {}", e.getMessage(), e);
			model.addAttribute("errorMessage", e.getMessage());
			return "5xx";
			//	throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
		}
	}


	@PostMapping("/{reservationId}/equipments")
	public String createEquipmentReservation(
				@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable("reservationId") Long reservationId,
			@ModelAttribute EquipmentFormReq formReq,
			RedirectAttributes redirectAttributes) {

		try {
			log.info("비품 예약 요청 - userId: {}, reservationId: {}", currentUser.getUserId(), reservationId);

			// 비품 선택 여부 확인
			if (!formReq.hasEquipments()) {
				redirectAttributes.addFlashAttribute("message", "비품을 선택해주세요.");
				return "redirect:/reservations/" + reservationId + "/equipments";
			}

			// Form DTO → Service DTO 변환
			AddEquipmentsReq request = formReq.toAddEquipmentsReq();
			log.info("변환된 비품 목록: {} 개", request.equipments().size());

			// Service 호출 - 비품 추가 pending상태로 예약
			EquipmentReservationRes response =
					reservationLockFacade.addEquipmentsToReservation(currentUser.getUserId(), reservationId, request);

			log.info("비품 추가 완료 - {} 종류", response.equipments().size());

			redirectAttributes.addFlashAttribute(
					"message",
					"비품이 추가되었습니다. \n승인 대기중입니다."
			);
			return "redirect:/reservations/rooms/" + reservationId;

		} catch (Exception e) {
			log.error("비품 예약 처리 중 예외 발생: {}", e.getMessage(), e);
			return "redirect:/common/error";
		}
	}


	/**
	 * 회의실 예약 확정 처리
	 */
	@PostMapping("/{reservationId}/confirm")
	public String confirmReservation(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId,
			@ModelAttribute ConfirmReservationReq request,
			RedirectAttributes redirectAttributes
	) {
		try {
			reservationService.confirmReservation(currentUser.getUserId(), reservationId, request);

			// 2. 확정된 예약 정보 조회 (CONFIRMED 비품 포함)
			ReservationRoomRes reservationRoom = reservationService.readConfirmedReservationRoom(currentUser, reservationId);

			// 3. 결제 정보 생성
			PaymentCheckoutReq checkoutReq = paymentService.createCheckoutInfo(reservationRoom);

			// 4. redirect로 전달
			redirectAttributes.addAttribute("amount", checkoutReq.amount());
			redirectAttributes.addAttribute("orderId", checkoutReq.orderId());
			redirectAttributes.addAttribute("orderName", checkoutReq.orderName());

			//비품 추가 건에서 확인TODO:
			redirectAttributes.addAttribute("reservationId", checkoutReq.reservationId());
			redirectAttributes.addAttribute("roomAmount", checkoutReq.roomAmount());
			redirectAttributes.addAttribute("equipmentAmount", checkoutReq.equipmentAmount());


			redirectAttributes.addFlashAttribute("alertType", "success");
			redirectAttributes.addFlashAttribute("message", "예약이 확정되었습니다.");

			return "redirect:/payment/checkout";
		//	return "redirect:/rooms";
		} catch (BusinessException e) {
			redirectAttributes.addFlashAttribute("alertType", "error");
			redirectAttributes.addFlashAttribute("message", e.getMessage());

			return "redirect:/reservations/rooms/" + reservationId;
		}
	}

	/**
	 * 뒤로 가기 기능 -> 회의실 예약 만료 처리
	 */
	@PostMapping("/{reservationId}/back/room")
	public String backFromRoom(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId) {
		reservationService.expireReservation(currentUser.getUserId(), reservationId);
		return "redirect:/rooms";
	}

	/**
	 * 회의실 예약 취소하기
	 */
	@PostMapping("/{reservationId}/cancel")
	public String cancelReservation(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId,
			@ModelAttribute CancelReservationReq request,
			RedirectAttributes redirectAttributes
	) {

		try {
			reservationService.cancelReservation(currentUser.getUserId(), reservationId, request);

			redirectAttributes.addFlashAttribute("alertType", "success");
			redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
		}
		catch (BusinessException e) {
			redirectAttributes.addFlashAttribute("alertType", "error");
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}

		return "redirect:/users/reservationlist";
	}

	/**
	 * 비품 추가 후, 예약 확인 페이지에서 이전 단계로 이동시 Pending을 Expired로 변경
	 */

	@PostMapping("/{reservationId}/back/equipment")
	public String backFromEquipment(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId,
			@RequestParam(required = false) List<Long> reservationEquipmentIds

	) {

		// 비품 ID 체크
		if (reservationEquipmentIds == null || reservationEquipmentIds.isEmpty()) {
			log.debug("비품 추가 페이지로 이동시 expire 대상 없음");
			return "redirect:/reservations/" + reservationId + "/equipments";
		}

		log.debug("backFromEquipment 호출");
		log.info("reservationId={}", reservationId);
		log.info("reservationEquipmentIds={}", reservationEquipmentIds);

		reservationService.expirePendingEquipments(currentUser.getUserId(), reservationEquipmentIds);
		return "redirect:/reservations/" + reservationId + "/equipments";
	}


	/**
	 * 회의실 기존 예약 건에 대한 비품 예약 취소
	 */
	@PostMapping("/{reservationId}/equipments/cancel")
	public String cancelReservationEquipments(
			@AuthenticationPrincipal CustomUser currentUser,
			@PathVariable Long reservationId,
			@ModelAttribute CancelReservationEquipmentsReq request,
			RedirectAttributes redirectAttributes
	) {
		log.info("비품 예약 취소 요청 - reservationId: {}, userId: {}",
				reservationId, currentUser.getUserId());
		log.debug(" Request: {}", request);  // 어떤 필드가 실제로 바인딩됐는지 확인
		log.debug(" reservationEquipmentIds: {}", request.reservationEquipmentIds());

		reservationService.cancelReservationEquipments(currentUser.getUserId(), reservationId, request);
		log.info("비품 예약 취소 완료 - reservationId: {}, userId: {}",
				reservationId, currentUser.getUserId());
		redirectAttributes.addFlashAttribute("message", "비품 예약이 취소되었습니다.");

		return "redirect:/users/reservationlist";
	}
}
