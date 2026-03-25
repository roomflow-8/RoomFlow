package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.request.CreateRoomSlotsReq;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRoomService {
    MeetingRoomListRes getMeetingRoomsByDate(LocalDate date);
    void generateSlots(LocalDate targetDate);
    void generateSlotsForRoom(CreateRoomSlotsReq createRoomSlotsReq);
}
