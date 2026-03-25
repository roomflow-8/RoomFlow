package com.goorm.roomflow.domain.reservation.controller;

import com.goorm.roomflow.domain.equipment.dto.EquipmentAvailabilityDto;
import com.goorm.roomflow.domain.equipment.dto.response.EquipmentListRes;
import com.goorm.roomflow.domain.equipment.service.EquipmentService;
import com.goorm.roomflow.domain.reservation.dto.request.*;
import com.goorm.roomflow.domain.reservation.dto.response.EquipmentReservationRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.service.ReservationLockFacade;
import com.goorm.roomflow.domain.reservation.service.ReservationService;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EquipmentService equipmentService;

    @PostMapping
    public String createRoomReservation(@ModelAttribute CreateReservationRoomReq request) {

        Long reservationId = reservationLockFacade.createReservationRoom(request).reservationId();

        return "redirect:/reservations/rooms/" + reservationId;
    }

    @GetMapping("/rooms/{reservationId}")
    public String reservationPage(@PathVariable("reservationId") Long reservationId,
                                  Model model) {

        ReservationRoomRes reservationRoomRes = reservationService.readReservationRoom(reservationId);

        model.addAttribute("reservationRoom", reservationRoomRes);
        return "reservation/confirm";
    }

    /**
     * 비품 선택 페이지
     * @param reservationId 예약 ID
     * @param model 모델
     * @return 비품 선택 페이지
     */
    @GetMapping("/{reservationId}/equipments")
    public String readAvailableEquipments(@PathVariable Long reservationId, Model model) {


        try {

            // 1. 예약 정보 조회
            Reservation reservation = reservationService.getReservation(reservationId);

            // 2. 사용 가능한 비품 목록 조회
            List<EquipmentAvailabilityDto> equipments =
                    reservationService.getAvailableEquipments(reservationId);

            log.info("비품 선택 페이지 요청 - reservationId: {}", reservationId);
            log.info("비품 목록 조회 완료 - {} 개", equipments.size());

            model.addAttribute("reservation", reservation);
            model.addAttribute("reservationId", reservationId);
            model.addAttribute("reservationStatus", reservation.getStatus());
            model.addAttribute("equipments", equipments);
            return "equipment/list";
        }catch (Exception e) {
            log.error("비품 목록 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return "common/error";
        }
    }


    /*
     비품 추가 페이지에서 비품 신청 완료 버튼 클릭시
     (회의실) 예약 상태가
     - Confirmed일 때 마이페이지로 이동
     - Pending일 때 예약확인 페이지(/reservations/rooms/{reservationId})로 이동
     */
    @PostMapping("/{reservationId}/equipments")
    public String createEquipmentReservation(@PathVariable("reservationId") Long reservationId,
                                             @ModelAttribute EquipmentFormReq formReq){

        try {
            log.info("비품 예약 요청 - reservationId: {}", reservationId);
            log.debug("받은 Form 데이터: {}", formReq);
            log.debug("equipments Map: {}", formReq.getEquipments());

            // 1. 예약 정보 조회
            Reservation reservation = reservationService.getReservation(reservationId);

            //예약 상태 확인
            ReservationStatus currentStatus = reservation.getStatus();

            log.info("*********현재 예약 상태: {}", currentStatus);


            // 2. 비품 선택 여부 확인
            if (!formReq.hasEquipments()) {
                return "redirect:/reservations/" + reservationId + "/equipments";
            }

            // 3. Form DTO → Service DTO 변환
            AddEquipmentsReq request = formReq.toAddEquipmentsReq();
            log.info("변환된 비품 목록: {} 개", request.equipments().size());

            // 4. Service 호출 - 비품 추가 pending상태로 예약
            EquipmentReservationRes response =
                    reservationService.addEquipmentsToReservation(reservationId, request);

            log.info("비품 추가 완료 - {} 종류", response.equipments().size());

            // 5. 예약 상태에 따라 분기
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                List<Long> equipmentIds = response.equipments().stream()
                        .map(e -> e.reservationEquipmentId())
                        .toList();

                reservationService.confirmEquipmentsService(reservationId, equipmentIds);

                //return "redirect:/mypage/reservations/" + reservationId;
                return "redirect:/reservations/rooms/" + reservationId;

            } else if(reservation.getStatus() == ReservationStatus.PENDING) {

                log.info("PENDING 예약에 비품 {} 개 추가 완료 (승인 대기)", response.equipments().size());
                return "redirect:/reservations/rooms/" + reservationId;
            } else {
                log.warn("처리 불가능한 예약 상태: {}", currentStatus);
                return "redirect:/rooms";
            }

        } catch (Exception e) {
            log.error("비품 예약 처리 중 예외 발생: {}", e.getMessage(), e);
            return "redirect:/common/error";
        }
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

    @PostMapping("/{reservationId}/back")
    public String goBackAndExpire(@PathVariable Long reservationId) {
        reservationService.expireReservation(reservationId);
        return "redirect:/rooms";
    }
}
