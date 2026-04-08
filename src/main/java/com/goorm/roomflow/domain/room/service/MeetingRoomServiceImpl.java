package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.response.*;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.mapper.MeetingRoomMapper;
import com.goorm.roomflow.domain.room.mapper.RoomSlotMapper;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomSlotRepository roomSlotRepository;

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
                        .comparingInt(this::getPriority)
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

    // 회의실 상태 여부에 따른 상태 메시지 반환
    private String getStatusMessage(MeetingRoom meetingRoom, List<RoomSlotRes> roomSlots) {
        RoomStatus status = meetingRoom.getStatus();

        if (status == RoomStatus.INACTIVE || status == RoomStatus.MAINTENANCE) {
            return status.getLabel();
        }

        boolean hasReservableSlot = roomSlots.stream().anyMatch(RoomSlotRes::isActive);

        if (!hasReservableSlot) {
            return "예약 마감";
        }

        return status.getLabel();
    }

    // 회의실 정렬 우선순위 반환
    private int getPriority(MeetingRoomRes room) {

        RoomStatus status = room.status();

        if ("예약 마감".equals(room.statusMessage())) {
            return 1;
        }

        return status.getPriority();
    }

}
