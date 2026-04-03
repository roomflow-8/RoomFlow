package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomAdminRes;
import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rooms")
public class AdminMeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping
    public String roomList(Model model) {

        List<MeetingRoomAdminRes> meetingRoomAdminListRes = meetingRoomService.readMeetingRoomAdminList();

        model.addAttribute("rooms", meetingRoomAdminListRes);

        return "admin/system/room-list";
    }
}
