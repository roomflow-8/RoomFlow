package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.service.ReservationLockFacade;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationLockFacade reservationLockFacade;

    /**
     * 회의실 예약 생성 처리
     */
    @PostMapping
    public String createRoomReservation(
            @SessionAttribute(name="loginUser", required=false) UserTO loginUser,
            @ModelAttribute CreateReservationRoomReq request) {

        Long reservationId = reservationLockFacade.createReservationRoom(loginUser.getUserId(), request).reservationId();

        return "redirect:/reservations/rooms/" + reservationId;
    }

    /**
     * 회의실 예약 확인 페이지 조회
     */
    @GetMapping("/rooms/{reservationId}")
    public String reservationPage(
            @SessionAttribute(name="loginUser", required=false) UserTO loginUser,
            @PathVariable("reservationId") Long reservationId,
            Model model) {

        ReservationRoomRes reservationRoomRes = reservationService.readReservationRoom(loginUser.getUserId(), reservationId);

        model.addAttribute("reservationRoom", reservationRoomRes);
        return "reservation/confirm";
    }

    /**
     * 회의실 예약 확정 처리
     */
    @PostMapping("/{reservationId}/confirm")
    public String confirmReservation(
            @SessionAttribute(name="loginUser", required=false) UserTO loginUser,
            @PathVariable Long reservationId,
            @ModelAttribute ConfirmReservationReq request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservationService.confirmReservation(loginUser.getUserId(), reservationId, request);
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("message", "예약이 확정되었습니다.");

            return "redirect:/rooms";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("message", e.getMessage());

            return "redirect:/reservations/rooms/" + reservationId;
        }
    }

    /**
     * 회의실 예약 만료 처리
     */
    @PostMapping("/{reservationId}/back")
    public String goBackAndExpire(
            @SessionAttribute(name="loginUser", required=false) UserTO loginUser,
            @PathVariable Long reservationId) {
        reservationService.expireReservation(loginUser.getUserId(), reservationId);
        return "redirect:/rooms";
    }
}
