package com.goorm.roomflow.domain.room.repository;

import com.goorm.roomflow.domain.room.entity.RoomSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface RoomSlotRepository extends JpaRepository<RoomSlot, Long> {
    List<RoomSlot> findBySlotStartAtGreaterThanEqualAndSlotStartAtLessThanOrderBySlotStartAtAsc(
            LocalDateTime startAt, LocalDateTime endAt);
}
