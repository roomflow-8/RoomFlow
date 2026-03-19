package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;
import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping
    public String rooms(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Model model
    ) {
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();

        MeetingRoomListRes meetingRoomListRes =
                meetingRoomService.getMeetingRoomsByDate(selectedDate);

        model.addAttribute("meetingRoomList", meetingRoomListRes);
        model.addAttribute("selectedDate", selectedDate);

        return "meeting-room/list";
    }
}
