package com.goorm.roomflow.domain.room.scheduler;

import com.goorm.roomflow.domain.room.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSlotScheduler {

    private final MeetingRoomService meetingRoomService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void generateOneMonthLaterSlot() {
        long start = System.currentTimeMillis();
        LocalDate targetDate = LocalDate.now().plusMonths(1);

        log.info("[RoomSlotScheduler] 슬롯 생성 시작 - targetDate={}", targetDate);

        try {
            meetingRoomService.generateSlots(targetDate);
            log.info("[RoomSlotScheduler] 슬롯 생성 완료 - targetDate={}, elapsed={}ms",
                    targetDate, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[RoomSlotScheduler] 슬롯 생성 실패 - targetDate={}, elapsed={}ms",
                    targetDate, System.currentTimeMillis() - start, e);
            throw e;
        }
    }
}
