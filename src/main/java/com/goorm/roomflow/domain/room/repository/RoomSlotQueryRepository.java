package com.goorm.roomflow.domain.room.repository;

import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotRes;
import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotSummaryRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;

import java.time.LocalDate;
import java.util.List;

public interface RoomSlotQueryRepository {

    List<AdminRoomSlotRes> findAdminRoomSlots(Long roomId, LocalDate date);
    AdminRoomSlotSummaryRes findRoomSlotSummary(MeetingRoom meetingRoom, LocalDate selectedDate);
    boolean existsValidReservationByRoomSlotId(Long roomSlotId);
    boolean existsValidReservationByRoomIdAndDate(Long roomId, LocalDate date);
}
