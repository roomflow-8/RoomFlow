package com.goorm.roomflow.domain.room.repository;

import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
}