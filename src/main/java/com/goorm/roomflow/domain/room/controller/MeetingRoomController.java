package com.goorm.roomflow.domain.room.controller;

import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomRepository meetingRoomRepository;

    @GetMapping("/api/rooms")
    public List<MeetingRoom> getRooms() {
        return meetingRoomRepository.findAll();
    }
}