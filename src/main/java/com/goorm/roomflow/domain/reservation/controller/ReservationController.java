package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
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

    @PostMapping
    public String createRoomReservation(@ModelAttribute CreateReservationRoomReq request) {

        Long reservationId = reservationService.createReservationRoom(request).reservationId();

        return "redirect:/reservations/rooms/" + reservationId;
    }

    @GetMapping("/rooms/{reservationId}")
    public String reservationPage(@PathVariable("reservationId") Long reservationId,
                                  Model model) {

        ReservationRoomRes reservationRoomRes = reservationService.readReservationRoom(reservationId);

        model.addAttribute("reservationRoom", reservationRoomRes);
        return "reservation/confirm";
    }

    @PostMapping("/{reservationId}/confirm")
    public String confirmReservation(
            @PathVariable Long reservationId,
            @ModelAttribute ConfirmReservationReq request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservationService.confirmReservation(reservationId, request);
            redirectAttributes.addFlashAttribute("alertType", "success");
            redirectAttributes.addFlashAttribute("message", "예약이 확정되었습니다.");

            return "redirect:/rooms";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("message", e.getMessage());

            return "redirect:/reservations/rooms/" + reservationId;
        }
    }
}
