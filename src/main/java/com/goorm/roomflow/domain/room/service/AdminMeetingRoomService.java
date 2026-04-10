package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.request.AdminMeetingRoomReq;
import com.goorm.roomflow.domain.room.dto.response.AdminMeetingRoomRes;
import com.goorm.roomflow.domain.room.entity.RoomStatus;

import java.util.List;

public interface AdminMeetingRoomService {
    List<AdminMeetingRoomRes> readMeetingRoomAdminList();
    void createMeetingRoom(AdminMeetingRoomReq adminMeetingRoomReq);
    void modifyMeetingRoom(Long roomId, AdminMeetingRoomReq adminMeetingRoomReq);
    void changeMeetingRoomStatus(Long roomId, RoomStatus targetStatus);
}
