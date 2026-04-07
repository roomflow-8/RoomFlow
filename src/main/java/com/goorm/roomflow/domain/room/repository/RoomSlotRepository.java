package com.goorm.roomflow.domain.room.repository;

import com.goorm.roomflow.domain.room.dto.response.RoomSlotAdminRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomSlotRepository extends JpaRepository<RoomSlot, Long> {
    List<RoomSlot> findBySlotStartAtGreaterThanEqualAndSlotStartAtLessThanOrderBySlotStartAtAsc(
            LocalDateTime startAt, LocalDateTime endAt);

    List<RoomSlot> findByMeetingRoomInAndSlotStartAtGreaterThanEqualAndSlotEndAtLessThanEqual(
            List<MeetingRoom> meetingRooms,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
        select rs
        from RoomSlot rs
        where rs.meetingRoom.roomId = :roomId
          and rs.roomSlotId in :slotIds
          and rs.isActive = true
        order by rs.slotStartAt asc
    """)
    List<RoomSlot> findActiveSlotsByRoomIdAndRoomSlotIds(
            @Param("roomId") Long roomId,
            @Param("slotIds") List<Long> slotIds
    );

    List<RoomSlot> findAllByMeetingRoomRoomIdAndSlotStartAtBetween(
            Long roomId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
}
