package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.dto.request.AdminRoomSlotDateStatusReq;
import com.goorm.roomflow.domain.room.dto.request.AdminRoomSlotGenerateReq;
import com.goorm.roomflow.domain.room.dto.request.AdminRoomSlotStatusReq;
import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotPageRes;
import com.goorm.roomflow.domain.room.service.AdminRoomSlotService;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rooms/{roomId}/slots")
public class AdminRoomSlotController {

    private final AdminRoomSlotService adminRoomSlotService;

    /**
     * 특정 회의실의 특정 날짜 슬롯 목록 조회
     */
    @GetMapping
    public String roomSlotList(
            @PathVariable Long roomId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        AdminRoomSlotPageRes page = adminRoomSlotService.getAdminRoomSlots(roomId, targetDate);

        model.addAttribute("roomId", roomId);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("page", page);

        model.addAttribute("generateForm", new AdminRoomSlotGenerateReq(targetDate));
        model.addAttribute("dateStatusForm", new AdminRoomSlotDateStatusReq(targetDate, true));

        return "admin/system/roomslot-list";
    }

    /**
     * 특정 날짜 슬롯 생성
     */
    @PostMapping("/generate")
    public String generateRoomSlots(
            @PathVariable Long roomId,
            @ModelAttribute("generateForm") AdminRoomSlotGenerateReq request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminRoomSlotService.generateRoomSlots(roomId, request.date());

            redirectAttributes.addFlashAttribute("message", "슬롯이 생성되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (IllegalArgumentException | BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/rooms/" + roomId + "/slots?date=" + request.date();
    }

    /**
     * 특정 슬롯 상태 변경
     */
    @PostMapping("/{roomSlotId}/status")
    public String changeRoomSlotStatus(
            @PathVariable Long roomId,
            @PathVariable Long roomSlotId,
            @ModelAttribute AdminRoomSlotStatusReq request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminRoomSlotService.changeRoomSlotStatus(roomId, roomSlotId, request.active());

            redirectAttributes.addFlashAttribute("message", "슬롯 상태가 변경되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (IllegalArgumentException | BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/rooms/" + roomId + "/slots?date=" + request.date();
    }

    /**
     * 특정 날짜 전체 슬롯 상태 변경
     */
    @PostMapping("/date-status")
    public String changeDateRoomSlotStatus(
            @PathVariable Long roomId,
            @ModelAttribute("dateStatusForm") AdminRoomSlotDateStatusReq request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminRoomSlotService.changeDateRoomSlotStatus(roomId, request.date(), request.active());

            redirectAttributes.addFlashAttribute("message", "해당 날짜 슬롯 상태가 변경되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");
        } catch (IllegalArgumentException | BusinessException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/rooms/" + roomId + "/slots?date=" + request.date();
    }
}
