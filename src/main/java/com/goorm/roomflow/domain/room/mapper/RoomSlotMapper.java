package com.goorm.roomflow.domain.room.mapper;

import com.goorm.roomflow.domain.room.dto.response.RoomSlotRes;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomSlotMapper {

    @Mapping(source = "active", target = "isActive")
    RoomSlotRes toResponse(RoomSlot roomSlot);
}