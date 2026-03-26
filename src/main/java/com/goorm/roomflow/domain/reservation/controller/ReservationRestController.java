package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.reservation.dto.request.*;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.service.ReservationLockFacade;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import com.goorm.roomflow.domain.user.dto.UserTO;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reservation API", description = "회의실/비품 예약 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationRestController {

    private final ReservationService reservationService;
    private final ReservationLockFacade reservationLockFacade;

    /**
     * 회의실 예약 생성 API
     */
    @Operation(summary = "회의실 예약 생성")
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ReservationRoomRes>> createReservationRoom(
            @SessionAttribute(name="loginUser", required=false) UserTO loginUser,
            @RequestBody CreateReservationRoomReq request) {

        ReservationRoomRes reservationRoomRes = reservationLockFacade.createReservationRoom(loginUser.getUserId(), request);

        return ApiResponse.success(
                SuccessCode.RESERVATION_CREATED,
                reservationRoomRes
        );
    }

    /**
     * 회의실 예약 조회 API
     */
    @Operation(summary = "회의실 예약 조회")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<ReservationRoomRes>> readReservationRoom(
            @SessionAttribute(name = "loginUser", required=false) UserTO loginUser,
            @RequestParam Long reservationId) {
        ReservationRoomRes reservationRoomRes = reservationService.readReservationRoom(loginUser.getUserId(), reservationId);

        return ApiResponse.success(
                SuccessCode.RESERVATION_SUCCESS,
                reservationRoomRes
        );
    }

    @Operation(summary = "비품 예약")
    @PostMapping("/{reservationId}/equipments")
    public ResponseEntity<ApiResponse<EquipmentReservationRes>> addEquipments(@PathVariable Long reservationId,
                                                                    @Valid @RequestBody AddEquipmentsReq request) {

        log.info("비품 예약 요청 - reservationId: {}, count: {}", reservationId, request.equipments().size());
        log.info("비품 예약 요청 ReservationRestController POST /api/v1/reservations/{}/equipments", reservationId);

        EquipmentReservationRes result = reservationService.addEquipmentsToReservation(reservationId, request);

        return ApiResponse.success(
                SuccessCode.EQUIPMENT_ADDED,
                result
        );

    }

    /**
     * 회의실 예약 확정 API
     */
    @Operation(summary = "회의실 예약 확정")
    @PatchMapping("/{reservationId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(
            @SessionAttribute(name = "loginUser", required=false) UserTO loginUser,
            @PathVariable Long reservationId,
            @RequestBody ConfirmReservationReq request) {
        log.info("loginUser: {}, reservationId: {}", loginUser.getUserId(), reservationId);

        reservationService.confirmReservation(loginUser.getUserId(), reservationId, request);

        return ApiResponse.success(
                SuccessCode.RESERVATION_SUCCESS
        );
    }

    /*
    * 회의실 예약 취소 API
    */
    @Operation(summary = "회의실 예약 취소")
    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @SessionAttribute(name = "loginUser", required=false) UserTO loginUser,
            @PathVariable Long reservationId,
            @RequestBody CancelReservationReq request
    ) {
        reservationService.cancelReservation(loginUser.getUserId(), reservationId, request);
        return ApiResponse.success(SuccessCode.RESERVATION_CANCELLED);
    }

    @Operation(summary = "회의실 기존 예약 건에 대한 비품 예약 확정")
    @PostMapping("/{reservationId}/equipments/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservationEquipments(@PathVariable Long reservationId, @RequestBody @Valid ConfirmReservationReq request) {
        log.info("비품 예약 확정 요청 - reservationId: {}, equipmentIds: {}",
                reservationId, request.reservationEquipmentIds());

        reservationService.confirmEquipmentsService(reservationId, request.reservationEquipmentIds());

        return ApiResponse.success(SuccessCode.EQUIPMENT_ADDED);
    }

    @Operation(summary = "회의실 기존 예약 건에 대한 비품 예약 취소")
    @PostMapping("/{reservationId}/equipments/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservationEquipments(@PathVariable Long reservationId, @RequestBody @Valid CancelReservationEquipmentsReq request) {
        log.info("비품 예약 취소 요청 - reservationId: {}, equipmentIds: {}",
                reservationId, request.reservationEquipmentIds());

        reservationService.cancelReservationEquipments(reservationId, request);

        return ApiResponse.success(SuccessCode.EQUIPMENT_CANCELLED);
    }
}
