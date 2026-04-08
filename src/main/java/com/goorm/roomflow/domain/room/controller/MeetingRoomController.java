package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.holiday.dto.response.HolidayCalendarRes;
import com.goorm.roomflow.domain.holiday.service.HolidayService;
import com.goorm.roomflow.domain.notice.service.NoticeService;
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
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;
    private final NoticeService noticeService;
    private final HolidayService holidayService;

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

        LocalDate startDate = selectedDate.withDayOfYear(1);
        LocalDate endDate = selectedDate.withDayOfYear(selectedDate.lengthOfYear());

        List<HolidayCalendarRes> holidays = holidayService.getHolidayDatesForCalendar(startDate, endDate);

        model.addAttribute("meetingRoomList", meetingRoomListRes);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("holidays", holidays);
        model.addAttribute("notice", noticeService.findPreviewNotice());

        return "meeting-room/list";
    }
}
