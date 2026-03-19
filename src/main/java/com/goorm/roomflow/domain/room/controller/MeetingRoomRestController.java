package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;
import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MeetingRoomRestController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<MeetingRoomListRes>> getMeetingRoomsByDate(@RequestParam LocalDate date) {

        MeetingRoomListRes meetingRoomListRes = meetingRoomService.getMeetingRoomsByDate(date);

        return ApiResponse.success(
                SuccessCode.RESERVATION_SUCCESS,
                meetingRoomListRes
        );
    }
}