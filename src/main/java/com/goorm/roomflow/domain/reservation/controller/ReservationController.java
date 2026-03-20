package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        return "reservation/reservation-form";
    }
}
