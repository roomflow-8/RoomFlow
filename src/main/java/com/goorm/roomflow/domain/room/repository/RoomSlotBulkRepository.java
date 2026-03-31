package com.goorm.roomflow.domain.room.repository;

import com.goorm.roomflow.domain.room.dto.RoomSlotInsertDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoomSlotBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsert(List<RoomSlotInsertDto> slots) {
        String sql = """
            INSERT INTO room_slots (room_id, slot_start_at, slot_end_at, is_active)
            VALUES (?, ?, ?, ?)
        """;

        jdbcTemplate.batchUpdate(sql, slots, slots.size(),
                (ps, slot) -> {
                    ps.setLong(1, slot.roomId());
                    ps.setObject(2, slot.slotStartAt());
                    ps.setObject(3, slot.slotEndAt());
                    ps.setBoolean(4, slot.isActive());
                });
    }
}
