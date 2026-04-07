package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.dto.request.MeetingRoomReq;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomAdminRes;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rooms")
public class AdminMeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    /**
     * 회의실 목록 조회
     */
    @GetMapping
    public String meetingRoomList(Model model) {
        loadRoomList(model);
        initForms(model);
        return "admin/system/room-list";
    }

    /**
     * 회의실 생성
     */
    @PostMapping("/create")
    public String createMeetingRoom(
            @ModelAttribute("createForm") MeetingRoomReq request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {
            meetingRoomService.createMeetingRoom(request);

            redirectAttributes.addFlashAttribute("message", "회의실이 생성되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

            return "redirect:/admin/rooms";

        } catch (IllegalArgumentException | BusinessException e) {

            loadRoomList(model);
            prepareModifyForm(model);

            model.addAttribute("openCreateModal", true);
            model.addAttribute("createErrorMessage", e.getMessage());

            return "admin/system/room-list";
        }
    }

    /**
     * 회의실 수정
     */
    @PostMapping("/{roomId}/edit")
    public String modifyMeetingRoom(
            @PathVariable Long roomId,
            @ModelAttribute("modifyForm") MeetingRoomReq request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {
            meetingRoomService.modifyMeetingRoom(roomId, request);

            redirectAttributes.addFlashAttribute("message", "회의실 정보가 수정되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

            return "redirect:/admin/rooms";

        } catch (IllegalArgumentException | BusinessException e) {

            loadRoomList(model);
            prepareCreateForm(model);

            model.addAttribute("openModifyModal", true);
            model.addAttribute("modifyTargetId", roomId);
            model.addAttribute("modifyErrorMessage", e.getMessage());

            return "admin/system/room-list";
        }
    }

    /**
     * 회의실 상태 변경
     */
    @PostMapping("/{roomId}/change")
    public String changeMeetingRoomStatus(
            @PathVariable Long roomId,
            @RequestParam("status") RoomStatus targetStatus,
            RedirectAttributes redirectAttributes
    ) {

        try {
            meetingRoomService.changeMeetingRoomStatus(roomId, targetStatus);

            redirectAttributes.addFlashAttribute("message", "회의실 상태가 변경되었습니다.");
            redirectAttributes.addFlashAttribute("alertType", "success");

        } catch (BusinessException e) {

            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("alertType", "error");
        }

        return "redirect:/admin/rooms";
    }


    private void loadRoomList(Model model) {
        List<MeetingRoomAdminRes> rooms = meetingRoomService.readMeetingRoomAdminList();
        model.addAttribute("rooms", rooms);
    }

    private void initForms(Model model) {
        prepareCreateForm(model);
        prepareModifyForm(model);
    }

    private void prepareCreateForm(Model model) {
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm",
                    new MeetingRoomReq(null, 0, null, null, null, null));
        }
    }

    private void prepareModifyForm(Model model) {
        if (!model.containsAttribute("modifyForm")) {
            model.addAttribute("modifyForm",
                    new MeetingRoomReq(null, 0, null, null, null, null));
        }
    }
}