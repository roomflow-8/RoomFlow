package com.goorm.roomflow.domain.room.scheduler;

import com.goorm.roomflow.domain.room.service.RoomSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSlotScheduler {

    private final RoomSlotService roomSlotService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void generateOneMonthLaterSlot() {
        LocalDate targetDate = LocalDate.now().plusMonths(1);

        log.info("[RoomSlotScheduler] 슬롯 생성 스케줄 시작 - targetDate={}", targetDate);
        roomSlotService.generateDailySlotsForActiveRooms(targetDate);
    }
}
