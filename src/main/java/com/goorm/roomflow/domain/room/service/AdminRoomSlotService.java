package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotPageRes;

import java.time.LocalDate;

public interface AdminRoomSlotService {
    AdminRoomSlotPageRes getAdminRoomSlots(Long roomId, LocalDate date);
    void generateRoomSlots(Long roomId, LocalDate date);
    void changeRoomSlotStatus(Long roomId, Long roomSlotId, boolean active);
    void changeDateRoomSlotStatus(Long roomId, LocalDate date, boolean active);
}
