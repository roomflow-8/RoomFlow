package com.goorm.roomflow.domain.room.mapper;

import com.goorm.roomflow.domain.room.dto.response.RoomSlotRes;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-20T04:19:58+0900",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.4.jar, environment: Java 21.0.10 (Azul Systems, Inc.)"
)
@Component
public class RoomSlotMapperImpl implements RoomSlotMapper {

    @Override
    public RoomSlotRes toResponse(RoomSlot roomSlot) {
        if ( roomSlot == null ) {
            return null;
        }

        boolean isActive = false;
        Long roomSlotId = null;
        LocalDateTime slotStartAt = null;
        LocalDateTime slotEndAt = null;

        isActive = roomSlot.isActive();
        roomSlotId = roomSlot.getRoomSlotId();
        slotStartAt = roomSlot.getSlotStartAt();
        slotEndAt = roomSlot.getSlotEndAt();

        RoomSlotRes roomSlotRes = new RoomSlotRes( roomSlotId, slotStartAt, slotEndAt, isActive );

        return roomSlotRes;
    }
}
