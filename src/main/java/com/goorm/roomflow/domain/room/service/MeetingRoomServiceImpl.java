package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.response.MeetingRoomListRes;
import com.goorm.roomflow.domain.room.dto.response.MeetingRoomRes;
import com.goorm.roomflow.domain.room.dto.response.RoomSlotRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.mapper.MeetingRoomMapper;
import com.goorm.roomflow.domain.room.mapper.RoomSlotMapper;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomSlotRepository roomSlotRepository;

    private final MeetingRoomMapper meetingRoomMapper;
    private final RoomSlotMapper roomSlotMapper;

    @Override
    @Transactional(readOnly = true)
    public MeetingRoomListRes getMeetingRoomsByDate(LocalDate date) {

        List<MeetingRoom> meetingRooms =
                meetingRoomRepository.findByStatusIn(
                        RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        List<RoomSlot> roomSlots =
                roomSlotRepository.findBySlotStartAtGreaterThanEqualAndSlotStartAtLessThanOrderBySlotStartAtAsc(
                        startAt, endAt);

        Map<Long, List<RoomSlotRes>> roomSlotMap = roomSlots.stream()
                .collect(Collectors.groupingBy(
                        roomSlot -> roomSlot.getMeetingRoom().getRoomId(),
                        Collectors.mapping(
                                roomSlotMapper::toResponse,
                                Collectors.toList()
                        )
                ));


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
                        // 1. 상태 우선순위
                        .comparingInt((MeetingRoomRes r) -> getPriority(r))
                        // 2. 예약 수 내림차순
                        .thenComparing(Comparator.comparingInt(MeetingRoomRes::totalReservations).reversed())
                )
                .toList();

        long availableRoomCount = rooms.stream()
                .filter(room -> "예약 가능".equals(room.statusMessage()))
                .count();

        return new MeetingRoomListRes(date, availableRoomCount, rooms);
    }

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
