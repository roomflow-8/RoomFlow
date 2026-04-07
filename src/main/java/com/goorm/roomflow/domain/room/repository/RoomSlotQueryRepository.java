package com.goorm.roomflow.domain.room.repository;

import com.goorm.roomflow.domain.room.dto.response.RoomSlotAdminRes;
import com.goorm.roomflow.domain.room.dto.response.RoomSlotSummaryRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;

import java.time.LocalDate;
import java.util.List;

public interface RoomSlotQueryRepository {

    List<RoomSlotAdminRes> findAdminRoomSlots(Long roomId, LocalDate date);
    RoomSlotSummaryRes findRoomSlotSummary(MeetingRoom meetingRoom, LocalDate selectedDate);
    boolean existsValidReservationByRoomSlotId(Long roomSlotId);
    boolean existsValidReservationByRoomIdAndDate(Long roomId, LocalDate date);
}
