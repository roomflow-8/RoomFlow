package com.goorm.roomflow.domain.room.mapper;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomRes;
import com.goorm.roomflow.domain.room.dto.response.RoomSlotRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = RoomSlotMapper.class
)
public interface MeetingRoomMapper {

    default MeetingRoomRes toResponse(
            MeetingRoom meetingRoom,
            List<RoomSlotRes> roomSlots,
            String statusMessage
    ) {
        return new MeetingRoomRes(
                meetingRoom.getRoomId(),
                meetingRoom.getRoomName(),
                meetingRoom.getCapacity(),
                meetingRoom.getDescription(),
                meetingRoom.getHourlyPrice(),
                meetingRoom.getStatus(),
                statusMessage,
                meetingRoom.getImageUrl(),
                meetingRoom.getTotalReservations(),
                roomSlots
        );
    }
}
