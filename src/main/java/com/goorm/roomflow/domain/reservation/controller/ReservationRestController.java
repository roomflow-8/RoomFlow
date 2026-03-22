package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.request.CancelReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.ConfirmReservationReq;
import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationRestController {

    private final ReservationService reservationService;

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ReservationRoomRes>> createReservationRoom(@RequestBody CreateReservationRoomReq request) {

        ReservationRoomRes reservationRoomRes = reservationService.createReservationRoom(request);

        return ApiResponse.success(
                SuccessCode.RESERVATION_CREATED,
                reservationRoomRes
        );
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<ReservationRoomRes>> readReservationRoom(@RequestParam Long reservationId) {
        ReservationRoomRes reservationRoomRes = reservationService.readReservationRoom(reservationId);

        return ApiResponse.success(
                SuccessCode.RESERVATION_SUCCESS,
                reservationRoomRes
        );
    }

    @PatchMapping("/{reservationId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(@PathVariable Long reservationId, @RequestBody ConfirmReservationReq request) {

        reservationService.confirmReservation(reservationId, request);

        return ApiResponse.success(
                SuccessCode.RESERVATION_SUCCESS
        );
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable Long reservationId,
            @RequestBody CancelReservationReq request
    ) {
        reservationService.cancelReservation(reservationId, request);
        return ApiResponse.success(SuccessCode.RESERVATION_CANCELLED);
    }
}
