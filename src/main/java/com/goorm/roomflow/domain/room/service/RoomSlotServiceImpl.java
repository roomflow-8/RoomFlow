package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
import com.goorm.roomflow.domain.room.dto.RoomSlotInsertDto;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotBulkRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSlotServiceImpl implements RoomSlotService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final RoomSlotBulkRepository roomSlotBulkRepository;
    private final ReservationPolicyRepository reservationPolicyRepository;
    /**
     * 예약 가능한(AVAILABLE) 또는 점검중(MAINTENANCE) 상태의 전체 회의실에 대해
     *  지정된 날짜(targetDate)의 시간 슬롯을 생성한다.
     * - 예외 상황에 대하여 재시도 3회, 대기시간 1초 -> 2초 -> 4초
     * - 재시도 정책 : DB 연결 실패, 트랜잭션 생성 실패, 락 흭득 실패, 비관적 락 충돌
     * - 주로 스케줄러에서 다음 달 슬롯 생성을 위해 호출
     * @param targetDate 슬롯 생성 날짜
     */
    @Retryable(
            retryFor = {
                    CannotCreateTransactionException.class,
                    CannotAcquireLockException.class,
                    PessimisticLockingFailureException.class,
                    CannotGetJdbcConnectionException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    @Override
    public void generateDailySlotsForActiveRooms(LocalDate targetDate) {

        log.info("전체 회의실 슬롯 생성 시작: targetDate={}", targetDate);

        List<MeetingRoom> meetingRooms =
                meetingRoomRepository.findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        log.info("슬롯 생성 대상 회의실 조회 완료: roomCount={}", meetingRooms.size());

        generateSlotsForMeetingRooms(meetingRooms, targetDate);

        log.info("슬롯 생성 대상 회의실 조회 완료: roomCount={}", meetingRooms.size());
    }

    @Recover
    @Override
    public void recover(Exception e, LocalDate targetDate) {
        log.error("전체 회의실 슬롯 생성 최종 실패: targetDate={}", targetDate, e);
    }

    /**
     * 지정된 회의실 목록에 대해 특정 날짜(targetDate)의 시간 슬롯을 생성한다.
     * - 관리자가 특정 회의실의 슬롯 수동 생성할 때 사용
     * @param roomIds 슬롯 생성 대상 회의실 ID 목록
     * @param targetDate 슬롯 생성 대상 날짜
     */
    @Transactional
    @Override
    public void generateDailySlotsForRooms(List<Long> roomIds, LocalDate targetDate) {

        log.info("선택 회의실 슬롯 생성 시작: targetDate={}, roomIds={}", targetDate, roomIds);

        List<MeetingRoom> meetingRooms = meetingRoomRepository.findAllById(roomIds);
        generateSlotsForMeetingRooms(meetingRooms, targetDate);

        log.info("선택 회의실 슬롯 생성 완료: targetDate={}, roomCount={}", targetDate, meetingRooms.size());
    }

    /**
     * 신규 생성된 회의실에 대해 초기 예약 가능 시간 슬롯을 생성한다.
     *
     * @param meetingRoom 슬롯 생성 대상 회의실
     */
    @Transactional
    @Override
    public void createInitialSlotsForRoom(MeetingRoom meetingRoom) {

        log.info("신규 회의실 초기 슬롯 생성 시작: roomId={}", meetingRoom.getRoomId());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1);

        for (LocalDate targetDate = startDate; !targetDate.isAfter(endDate); targetDate = targetDate.plusDays(1)) {
            generateSlotsForMeetingRooms(List.of(meetingRoom), targetDate);
        }

        log.info("신규 회의실 초기 슬롯 생성 완료: roomId={}, startDate={}, endDate={}", meetingRoom.getRoomId(), startDate, endDate);
    }

    /**
     * 회의실 목록에 대해 특정 날짜(targetDate)의 시간 슬롯을 생성한다.
     *
     * @param meetingRooms 슬롯 생성 대상 회의실 목록
     * @param targetDate 슬롯 생성 대상 날짜
     */
    private void generateSlotsForMeetingRooms(List<MeetingRoom> meetingRooms, LocalDate targetDate) {
        if (meetingRooms == null || meetingRooms.isEmpty()) {
            log.warn("슬롯 생성 대상 회의실이 없습니다. targetDate={}", targetDate);
            return;
        }

        log.info("슬롯 생성 처리 시작: targetDate={}, roomCount={}", targetDate, meetingRooms.size());

        // 필요한 정책 조회
        SlotPolicy policy = readSlotPolicy();

        LocalDateTime dayStart = targetDate.atTime(policy.startTime());
        LocalDateTime dayEnd = targetDate.atTime(policy.endTime());

        // 기존에 있는 슬롯 조회
        List<RoomSlot> existingSlots =
                roomSlotRepository.findByMeetingRoomInAndSlotStartAtGreaterThanEqualAndSlotEndAtLessThanEqual(
                        meetingRooms, dayStart, dayEnd
                );

        log.info("기존 슬롯 조회 완료: existingSlotCount={}", existingSlots.size());

        Set<String> existingSlotKeys = existingSlots.stream()
                .map(slot -> generateSlotKey(
                        slot.getMeetingRoom().getRoomId(),
                        slot.getSlotStartAt(),
                        slot.getSlotEndAt()
                ))
                .collect(Collectors.toSet());

        List<RoomSlotInsertDto> insertTargets = new ArrayList<>();

        // 생성 가능한 슬롯 범위 계산
        for (MeetingRoom meetingRoom : meetingRooms) {
            LocalDateTime currentTime = dayStart;

            while (currentTime.isBefore(dayEnd)) {
                LocalDateTime nextTime = currentTime.plusMinutes(policy.slotUnitMinutes());

                String slotKey = generateSlotKey(meetingRoom.getRoomId(), currentTime, nextTime);

                if (!existingSlotKeys.contains(slotKey)) {
                    insertTargets.add(new RoomSlotInsertDto(
                            meetingRoom.getRoomId(),
                            currentTime,
                            nextTime,
                            true
                    ));
                }

                currentTime = nextTime;
            }
        }

        log.info("신규 슬롯 계산 완료: insertTargetCount={}", insertTargets.size());

        if (!insertTargets.isEmpty()) {
            roomSlotBulkRepository.bulkInsert(insertTargets);

            log.info("슬롯 bulk insert 완료: insertedCount={}", insertTargets.size());
        }
    }

    // 슬롯 생성에 필요한 정책 조회
    private SlotPolicy readSlotPolicy() {
        List<String> policyKeys = List.of(
                "RESERVATION_START_TIME",
                "RESERVATION_END_TIME",
                "SLOT_UNIT_MINUTES"
        );

        Map<String, String> policyMap = reservationPolicyRepository.findByPolicyKeyIn(policyKeys).stream()
                .collect(Collectors.toMap(
                        ReservationPolicy::getPolicyKey,
                        ReservationPolicy::getPolicyValue
                ));

        return new SlotPolicy(
                LocalTime.parse(policyMap.get("RESERVATION_START_TIME")),
                LocalTime.parse(policyMap.get("RESERVATION_END_TIME")),
                Integer.parseInt(policyMap.get("SLOT_UNIT_MINUTES"))
        );
    }

    // 슬롯 증복 여부 비교 - 고유 key 생성
    private String generateSlotKey(Long roomId, LocalDateTime startAt, LocalDateTime endAt) {
        return roomId + "|" + startAt + "|" + endAt;
    }

    private record SlotPolicy(
            LocalTime startTime,
            LocalTime endTime,
            int slotUnitMinutes
    ) {}

}