package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.reservation.service.ReservationPolicyService;
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
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private final ReservationPolicyService reservationPolicyService;

    @Override
    @Transactional(readOnly = true)
    public MeetingRoomListRes getMeetingRoomsByDate(LocalDate date) {

        List<MeetingRoom> meetingRooms =
                meetingRoomRepository.findByStatusIn(
                        RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        LocalDateTime startAt;

        if (date.equals(LocalDate.now())) {
            startAt = LocalDateTime.now();
        } else {
            startAt = date.atStartOfDay();
        }

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

    @Override
    @Transactional
    public void generateSlots(LocalDate targetDate) {

        List<MeetingRoom> meetingRooms = meetingRoomRepository.findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);

        generateSlotsForMeetingRooms(meetingRooms, targetDate);
    }

    @Override
    @Transactional
    public void generateSlotsForRoom(CreateRoomSlotsReq createRoomSlotsReq) {

        List<MeetingRoom> meetingRooms = meetingRoomRepository.findAllById(createRoomSlotsReq.meetingRoomIds());

        generateSlotsForMeetingRooms(meetingRooms, createRoomSlotsReq.targetDate());
    }

    private void generateSlotsForMeetingRooms(List<MeetingRoom> meetingRooms, LocalDate targetDate) {
        LocalTime startTime = reservationPolicyService.getTimeValue("RESERVATION_START_TIME");
        LocalTime endTime = reservationPolicyService.getTimeValue("RESERVATION_END_TIME");
        int slotUnitMinutes = reservationPolicyService.getIntValue("SLOT_UNIT_MINUTES");

        for (MeetingRoom meetingRoom : meetingRooms) {
            createSlotsIfNotExists(meetingRoom, targetDate, startTime, endTime, slotUnitMinutes);
        }

    }

    private void createSlotsIfNotExists(MeetingRoom meetingRoom, LocalDate targetDate,
                                        LocalTime startTime, LocalTime endTime, int slotUnitMinutes) {

        LocalDateTime currentTime = targetDate.atTime(startTime);
        LocalDateTime closeTime = targetDate.atTime(endTime);

        while(currentTime.isBefore(closeTime)) {
            LocalDateTime nextTime = currentTime.plusMinutes(slotUnitMinutes);

            boolean exists = roomSlotRepository.existsByMeetingRoomAndSlotStartAtAndSlotEndAt(
                    meetingRoom, currentTime, nextTime
            );

            if(!exists) {
                roomSlotRepository.save(
                    new RoomSlot(meetingRoom, currentTime, nextTime, true)
                );
            }

            currentTime = nextTime;
        }

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
