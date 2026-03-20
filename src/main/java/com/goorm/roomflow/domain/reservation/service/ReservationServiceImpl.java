package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.mapper.ReservationRoomMapper;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final ReservationRoomMapper reservationRoomMapper;

    @Override
    @Transactional(readOnly = true)
    public ReservationRoomRes readReservationRoom(Long reservationId) {

        // 1. 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        // 2. 회의실 예약 조회
        List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservation(reservation);

        if (reservationRooms.isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        MeetingRoom meetingRoom = reservation.getMeetingRoom();

        // 3. 시간 조회
        List<RoomSlot> roomSlots = reservationRooms.stream()
                .map(ReservationRoom::getRoomSlot)
                .sorted(Comparator.comparing(RoomSlot::getSlotStartAt))
                .toList();

        List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

        return reservationRoomMapper.toReservationRoomRes(
                reservation,
                meetingRoom,
                reservationTimeSlots
        );
    }

    @Override
    @Transactional
    public ReservationRoomRes createReservationRoom(CreateReservationRoomReq request) {

        // 1. 회의실 조회
        MeetingRoom meetingRoom = meetingRoomRepository.findById(request.roomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        if (meetingRoom.getStatus() != RoomStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        // 2. 멱등키 중복 체크
        if(reservationRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESERVATION_REQUEST);
        }

        // 3. 슬롯 조회
        List<RoomSlot> roomSlots = roomSlotRepository.findByMeetingRoom_RoomIdAndRoomSlotIdIn(
                request.roomId(), request.roomSlotIds());

        // 3-1. 슬롯 예외 처리
        if(roomSlots.size() != request.roomSlotIds().size()) {
            throw new BusinessException(ErrorCode.ROOM_SLOT_NOT_FOUND);
        }

        // 3-2. 슬롯 예약 확인
        boolean hasUnavailableSlot = roomSlots.stream()
                .anyMatch(slot -> !slot.isActive());

        if(hasUnavailableSlot) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_EXISTS);
        }

        // 4. 금액 계산
        BigDecimal totalAmount = calcTotalAmount(meetingRoom.getHourlyPrice(), roomSlots);

        // 5. 예약 생성
        Reservation reservation = new Reservation(
                1L,
                meetingRoom,
                request.idempotencyKey(),
                totalAmount);

        reservationRepository.save(reservation);

        // 6. 회의실 예약 생성
        List<ReservationRoom> reservationRooms = roomSlots.stream()
                .map(roomSlot -> new ReservationRoom(
                        reservation,
                        meetingRoom,
                        roomSlot,
                        meetingRoom.getHourlyPrice()
                )).toList();

        reservationRoomRepository.saveAll(reservationRooms);

        // 7. 슬롯 상태 변경
        roomSlots.forEach(roomSlot -> roomSlot.updateActiveStatus(false));

        // 8. 슬롯 정리
        List<ReservationTimeSlot> reservationTimeSlots = makeReservationTimeSlot(roomSlots);

        // 9. 결과 반환
        return reservationRoomMapper.toReservationRoomRes(
                reservation,
                meetingRoom,
                reservationTimeSlots
        );

    }

    // 슬롯 시간별 정리 함수
    private List<ReservationTimeSlot> makeReservationTimeSlot(List<RoomSlot> roomSlots) {
        List<RoomSlot> sortedSlots = roomSlots.stream()
                .sorted(Comparator.comparing(RoomSlot::getSlotStartAt))
                .toList();

        List<ReservationTimeSlot> reservationTimeSlots = new ArrayList<>();

        LocalDateTime start = sortedSlots.getFirst().getSlotStartAt();
        LocalDateTime end = sortedSlots.getFirst().getSlotEndAt();

        for(int i = 1; i < sortedSlots.size(); i++) {
            RoomSlot slot = sortedSlots.get(i);

            if(slot.getSlotStartAt().equals(end)) {
                end = slot.getSlotEndAt();
                continue;
            }

            reservationTimeSlots.add(new ReservationTimeSlot(start, end));

            start = slot.getSlotStartAt();
            end = slot.getSlotEndAt();
        }

        reservationTimeSlots.add(new ReservationTimeSlot(start, end));

        return reservationTimeSlots;
    }

    // 총 금액 계산
    private BigDecimal calcTotalAmount(BigDecimal houlyPrice, List<RoomSlot> roomSlots) {
        return houlyPrice.multiply(BigDecimal.valueOf(roomSlots.size()));
    }
}
