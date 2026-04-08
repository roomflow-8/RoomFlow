package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface RoomSlotService {
    void generateDailySlotsForActiveRooms(LocalDate targetDate);
    void recover(Exception e, LocalDate targetDate);
    void generateDailySlotsForRooms(List<Long> roomIds, LocalDate targetDate);
    void createInitialSlotsForRoom(MeetingRoom meetingRoom);
}
