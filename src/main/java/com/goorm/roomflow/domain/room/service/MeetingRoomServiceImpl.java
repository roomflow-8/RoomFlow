package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
import com.goorm.roomflow.domain.room.dto.RoomSlotInsertDto;
import com.goorm.roomflow.domain.room.dto.request.CreateRoomSlotsReq;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomRes;
import com.goorm.roomflow.domain.room.dto.response.RoomSlotRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.mapper.MeetingRoomMapper;
import com.goorm.roomflow.domain.room.mapper.RoomSlotMapper;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotBulkRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final RoomSlotBulkRepository roomSlotBulkRepository;
    private final ReservationPolicyRepository reservationPolicyRepository;

    private final MeetingRoomMapper meetingRoomMapper;
    private final RoomSlotMapper roomSlotMapper;

    /**
     * 날짜별 회의실 목록 조회
     *
     * @param date 조회 날짜
     * @return 날짜별 회의실 목록 응답 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public MeetingRoomListRes getMeetingRoomsByDate(LocalDate date) {

        log.info("회의실 목록 조회 시작: date={}", date);

        // 1. 조회 가능한 회의실 목록 조회
        List<MeetingRoom> meetingRooms =
                meetingRoomRepository.findByStatusIn(
                        RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        // 2. 조회 대상 시간 범위 계산 - 오늘이면 현재 시간 이후 / 미래 날짜면 하루 전체 조회
        LocalDateTime startAt;

        if (date.equals(LocalDate.now())) {
            startAt = LocalDateTime.now();
        } else {
            startAt = date.atStartOfDay();
        }

        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        // 3. 해당 날짜의 슬롯 조회
        List<RoomSlot> roomSlots =
                roomSlotRepository.findBySlotStartAtGreaterThanEqualAndSlotStartAtLessThanOrderBySlotStartAtAsc(
                        startAt, endAt);

        // 4. 회의실 ID 기준 슬롯 그룹핑
        Map<Long, List<RoomSlotRes>> roomSlotMap = roomSlots.stream()
                .collect(Collectors.groupingBy(
                        roomSlot -> roomSlot.getMeetingRoom().getRoomId(),
                        Collectors.mapping(
                                roomSlotMapper::toResponse,
                                Collectors.toList()
                        )
                ));


        // 5. 회의실 응답 DTO 생성
        List<MeetingRoomRes> rooms = meetingRooms.stream()
                .map(room -> {
                    List<RoomSlotRes> roomSlotResList =
                            roomSlotMap.getOrDefault(room.getRoomId(), List.of());

                    String statusMessage = getStatusMessage(room, roomSlotResList);

                    return meetingRoomMapper.toResponse(
                            room,
                            roomSlotResList,
                            statusMessage
                    );
                })
                .sorted(Comparator
                        // 상태 우선순위 정렬
                        .comparingInt((MeetingRoomRes r) -> getPriority(r))
                        // 예약 수 내림차순 정렬
                        .thenComparing(Comparator.comparingInt(MeetingRoomRes::totalReservations).reversed())
                )
                .toList();

        // 6. 예약 가능한 회의실 개수 집계
        long availableRoomCount = rooms.stream()
                .filter(room -> "예약 가능".equals(room.statusMessage()))
                .count();

        log.info("회의실 목록 조회 완료: date={}, totalRooms={}, availableRooms={}",
                date,
                rooms.size(),
                availableRoomCount
        );

        return new MeetingRoomListRes(date, availableRoomCount, rooms);
    }

    /**
     * 예약 가능한/점검중 회의실에 대해 특정 날짜의 시간 슬롯 생성
     *
     * @param targetDate 슬롯 생성 날짜
     */
    @Override
    @Transactional
    public void generateSlots(LocalDate targetDate) {

        log.info("전체 회의실 슬롯 생성 시작: targetDate={}", targetDate);

        // 예약 가능한 or 점검 중 회의실 조회
        List<MeetingRoom> meetingRooms = meetingRoomRepository.findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        generateSlotsForMeetingRooms(meetingRooms, targetDate);

        log.info("전체 회의실 슬롯 생성 완료: targetDate={}, roomCount={}", targetDate, meetingRooms.size());
    }

    /**
     * 요청한 회의실에 대해서만 특정 날짜의 슬롯 생성
     *
     * @param createRoomSlotsReq 회의실 ID 목록과 대상 날짜
     */
    @Override
    @Transactional
    public void generateSlotsForRoom(CreateRoomSlotsReq createRoomSlotsReq) {

        log.info("선택 회의실 슬롯 생성 시작: targetDate={}, roomIds={}",
                createRoomSlotsReq.targetDate(),
                createRoomSlotsReq.meetingRoomIds());

        List<MeetingRoom> meetingRooms = meetingRoomRepository.findAllById(createRoomSlotsReq.meetingRoomIds());

        generateSlotsForMeetingRooms(meetingRooms, createRoomSlotsReq.targetDate());

        log.info("선택 회의실 슬롯 생성 완료: targetDate={}, roomCount={}",
                createRoomSlotsReq.targetDate(),
                meetingRooms.size());
    }

    /**
     * 회의실 목록에 대해 특정 날짜의 슬롯을 생성
     *
     * @param meetingRooms 회의실 목록
     * @param targetDate 특정 날짜
     */
    private void generateSlotsForMeetingRooms(List<MeetingRoom> meetingRooms, LocalDate targetDate) {
        if (meetingRooms == null || meetingRooms.isEmpty()) {
            log.warn("슬롯 생성 대상 회의실이 없습니다. targetDate={}", targetDate);
            return;
        }

        log.info("슬롯 생성 처리 시작: targetDate={}, roomCount={}",
                targetDate, meetingRooms.size());

        // 1. 필요한 정책 일괄 조회
        List<String> policyKeys = List.of(
                "RESERVATION_START_TIME",
                "RESERVATION_END_TIME",
                "SLOT_UNIT_MINUTES"
        );

        List<ReservationPolicy> policies = reservationPolicyRepository.findByPolicyKeyIn(policyKeys);

        Map<String, String> policyMap = policies.stream()
                .collect(Collectors.toMap(
                        ReservationPolicy::getPolicyKey,
                        ReservationPolicy::getPolicyValue
                ));

        LocalTime startTime = LocalTime.parse(policyMap.get("RESERVATION_START_TIME"));
        LocalTime endTime = LocalTime.parse(policyMap.get("RESERVATION_END_TIME"));
        int slotUnitMinutes = Integer.parseInt(policyMap.get("SLOT_UNIT_MINUTES"));

        LocalDateTime dayStart = targetDate.atTime(startTime);
        LocalDateTime dayEnd = targetDate.atTime(endTime);

        // 2. 기존 슬롯 조회
        List<RoomSlot> existingSlots =
                roomSlotRepository.findByMeetingRoomInAndSlotStartAtGreaterThanEqualAndSlotEndAtLessThanEqual(
                        meetingRooms, dayStart, dayEnd
                );

        Set<String> existingSlotKeys = existingSlots.stream()
                .map(slot -> generateSlotKey(
                        slot.getMeetingRoom().getRoomId(),
                        slot.getSlotStartAt(),
                        slot.getSlotEndAt()
                ))
                .collect(Collectors.toSet());

        // 3. 생성할 대상 슬롯 수집
        List<RoomSlotInsertDto> insertTargets = new ArrayList<>();

        for (MeetingRoom meetingRoom : meetingRooms) {
            LocalDateTime currentTime = dayStart;

            while (currentTime.isBefore(dayEnd)) {
                LocalDateTime nextTime = currentTime.plusMinutes(slotUnitMinutes);

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

        log.info("신규 슬롯 계산 완료: targetDate={}, insertTargetCount={}",
                targetDate, insertTargets.size());

        if (!insertTargets.isEmpty()) {
            roomSlotBulkRepository.bulkInsert(insertTargets);

            log.info("슬롯 bulk insert 완료: targetDate={}, insertedCount={}",
                    targetDate, insertTargets.size());
            return;
        }
        log.info("추가 생성할 슬롯이 없습니다. targetDate={}", targetDate);
    }

    // 슬롯 증복 여부 비교 - 고유 key 생성
    private String generateSlotKey(Long roomId, LocalDateTime startAt, LocalDateTime endAt) {
        return roomId + "|" + startAt + "|" + endAt;
    }

    // 회의실 상태 여부에 따른 상태 메시지 반환
    private String getStatusMessage(MeetingRoom meetingRoom, List<RoomSlotRes> roomSlots) {
        if (meetingRoom.getStatus() == RoomStatus.MAINTENANCE) {
            return "점검 중";
        }

        boolean hasReservableSlot = roomSlots.stream().anyMatch(RoomSlotRes::isActive);

        if (!hasReservableSlot) {
            return "예약 마감";
        }

        return "예약 가능";
    }

    // 회의실 정렬 우선순위 반환
    private int getPriority(MeetingRoomRes room) {
        if ("예약 가능".equals(room.statusMessage())) {
            return 0;
        }
        if ("예약 마감".equals(room.statusMessage())) {
            return 1;
        }
        return 2;
    }

}
