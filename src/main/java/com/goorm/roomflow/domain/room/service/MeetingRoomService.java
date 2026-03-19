package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;

import java.time.LocalDate;

public interface MeetingRoomService {
    MeetingRoomListRes getMeetingRoomsByDate(LocalDate date);
}
