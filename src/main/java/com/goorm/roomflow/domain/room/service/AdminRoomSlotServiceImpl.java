package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotPageRes;
import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotRes;
import com.goorm.roomflow.domain.room.dto.response.AdminRoomSlotSummaryRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotQueryRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminRoomSlotServiceImpl implements AdminRoomSlotService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final RoomSlotService roomSlotService;
    private final RoomSlotQueryRepository roomSlotQueryRepository;

    /**
     * 관리자용 특정 날짜 슬롯 목록 조회
     *
     * 조회 데이터: 슬롯 시작/종료 시간, 활성 여부, 예약 여부(Pending, Confirm)
     */
    @Override
    public AdminRoomSlotPageRes getAdminRoomSlots(Long roomId, LocalDate date) {

        log.info("관리자 슬롯 조회 시작 - roomId={}, date={}", roomId, date);

        validateDate(date);
        MeetingRoom meetingRoom = getMeetingRoom(roomId);

        if(meetingRoom == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        AdminRoomSlotSummaryRes summary = roomSlotQueryRepository.findRoomSlotSummary(meetingRoom, date);
        List<AdminRoomSlotRes> slots = roomSlotQueryRepository.findAdminRoomSlots(roomId, date);

        log.info("관리자 슬롯 페이지 조회 완료 - roomId={}, date={}, slotCount={}",
                roomId, date, slots.size());

        return new AdminRoomSlotPageRes(summary, slots);
    }

    /**
     * 특정 날짜 슬롯 생성
     * 이미 존재하는 슬롯은 생성하지 않음
     */
    @Transactional
    @Override
    public void generateRoomSlots(Long roomId, LocalDate date) {

        log.info("슬롯 생성 시작 - roomId={}, date={}", roomId, date);
        validateDate(date);

        MeetingRoom meetingRoom = getMeetingRoom(roomId);

        // 사용 가능한 회의실만 슬롯 생성 허용
        if (meetingRoom.getStatus() == RoomStatus.INACTIVE) {
            log.warn("슬롯 생성 실패 - 비활성 회의실 roomId={}", roomId);
            throw new BusinessException(ErrorCode.ROOM_SLOT_GENERATION_NOT_ALLOWED);
        }

        roomSlotService.generateDailySlotsForRooms(List.of(roomId), date);

        log.info("슬롯 생성 완료 - roomId={}, date={}", roomId, date);
    }

    /**
     * 단건 슬롯 상태 변경
     *
     * 변경 가능 조건:
     * - 해당 회의실의 슬롯이어야 함
     * - 과거 슬롯 수정 불가
     * - 예약된 슬롯 비활성화 불가
     */
    @Transactional
    @Override
    public void changeRoomSlotStatus(Long roomId, Long roomSlotId, boolean active) {

        log.info("슬롯 상태 변경 시작 - roomId={}, roomSlotId={}, active={}", roomId, roomSlotId, active);

        MeetingRoom meetingRoom = getMeetingRoom(roomId);
        RoomSlot roomSlot = getRoomSlot(roomSlotId);

        validateRoomSlotOwner(meetingRoom, roomSlot);
        validateFutureSlot(roomSlot);

        // 예약된 슬롯 비활성화 방지
        if (!active &&
                roomSlotQueryRepository.existsValidReservationByRoomSlotId(roomSlotId)) {
            log.warn("슬롯 비활성화 실패 - 예약 존재 roomSlotId={}", roomSlotId);
            throw new BusinessException(ErrorCode.ROOM_SLOT_DATE_HAS_RESERVED_SLOT);
        }

        roomSlot.updateActiveStatus(active);

        log.info("슬롯 상태 변경 완료 - roomSlotId={}, active={}", roomSlotId, active);
    }

    /**
     * 특정 날짜 전체 슬롯 상태 변경
     *
     * 변경 가능 조건:
     * - 과거 슬롯 포함 시 실패
     * - 예약 포함 슬롯 존재 시 비활성화 실패
     */
    @Transactional
    @Override
    public void changeDateRoomSlotStatus(Long roomId, LocalDate date, boolean active) {

        log.info("날짜 전체 슬롯 상태 변경 시작 - roomId={}, date={}, active={}", roomId, date, active);

        validateDate(date);
        getMeetingRoom(roomId);

        if(date.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.ROOM_SLOT_PAST_NOT_MODIFIABLE);
        }

        List<RoomSlot> roomSlots = roomSlotRepository.findAllByMeetingRoomRoomIdAndSlotStartAtBetween(
                roomId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        if (roomSlots.isEmpty()) {
            log.warn("슬롯 상태 변경 실패 - 슬롯 없음 roomId={}, date={}", roomId, date);
            throw new BusinessException(ErrorCode.ROOM_SLOT_DATE_NOT_FOUND);
        }

        // 예약 포함 슬롯 존재 시 비활성화 금지
        if (!active && roomSlotQueryRepository.existsValidReservationByRoomIdAndDate(roomId, date)) {
            log.warn("슬롯 일괄 비활성화 실패 - 예약 포함 roomId={}, date={}", roomId, date);
            throw new BusinessException(ErrorCode.ROOM_SLOT_DATE_HAS_RESERVED_SLOT);
        }

        roomSlots.forEach(slot -> slot.updateActiveStatus(active));

        log.info("날짜 전체 슬롯 상태 변경 완료 - roomId={}, date={}, updatedCount={}", roomId, date, roomSlots.size());
    }


    private void validateRoomSlotOwner(MeetingRoom meetingRoom, RoomSlot roomSlot) {

        if (!roomSlot.getMeetingRoom().getRoomId().equals(meetingRoom.getRoomId())) {
            log.warn("슬롯 소유 회의실 불일치 - roomId={}, roomSlotId={}", meetingRoom.getRoomId(), roomSlot.getRoomSlotId());
            throw new BusinessException(ErrorCode.ROOM_SLOT_NOT_BELONG_TO_ROOM);
        }
    }


    private void validateFutureSlot(RoomSlot roomSlot) {

        if (roomSlot.getSlotStartAt().isBefore(LocalDateTime.now())) {
            log.warn("과거 슬롯 수정 시도 - roomSlotId={}", roomSlot.getRoomSlotId());
            throw new BusinessException(ErrorCode.ROOM_SLOT_PAST_NOT_MODIFIABLE);
        }
    }

    private RoomSlot getRoomSlot(Long roomSlotId) {

        return roomSlotRepository.findById(roomSlotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_SLOT_NOT_FOUND));
    }

    private MeetingRoom getMeetingRoom(Long roomId) {
        return meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }
    }
}
