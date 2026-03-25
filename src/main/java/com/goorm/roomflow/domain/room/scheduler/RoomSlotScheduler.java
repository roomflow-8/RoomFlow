package com.goorm.roomflow.domain.room.scheduler;

import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RoomSlotScheduler {

    private final MeetingRoomService meetingRoomService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void generateOneMonthLaterSlot() {

        LocalDate targetDate = LocalDate.now().plusMonths(1);

        meetingRoomService.generateSlots(targetDate);
    }
}
