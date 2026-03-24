package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;
import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import com.goorm.roomflow.global.code.SuccessCode;
import com.goorm.roomflow.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "MeetingRoom API", description = "회의실 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MeetingRoomRestController {

    private final MeetingRoomService meetingRoomService;

    @Operation(summary = "날짜별 회의실 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<MeetingRoomListRes>> getMeetingRoomsByDate(@RequestParam LocalDate date) {

        MeetingRoomListRes meetingRoomListRes = meetingRoomService.getMeetingRoomsByDate(date);

        return ApiResponse.success(
                SuccessCode.ROOM_SUCCESS,
                meetingRoomListRes
        );
    }
}