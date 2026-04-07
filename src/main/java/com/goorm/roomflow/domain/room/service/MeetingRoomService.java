package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.request.MeetingRoomReq;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomAdminRes;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRoomService {
    MeetingRoomListRes getMeetingRoomsByDate(LocalDate date);

    List<MeetingRoomAdminRes> readMeetingRoomAdminList();
    void createMeetingRoom(MeetingRoomReq meetingRoomReq);
    void modifyMeetingRoom(Long roomId, MeetingRoomReq meetingRoomReq );
    void changeMeetingRoomStatus(Long roomId, RoomStatus targetStatus);
}
