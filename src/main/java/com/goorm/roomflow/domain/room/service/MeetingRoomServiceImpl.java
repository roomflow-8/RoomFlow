package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.dto.RoomSlotInsertDto;
import com.goorm.roomflow.domain.room.dto.request.CreateRoomSlotsReq;
import com.goorm.roomflow.domain.room.dto.request.MeetingRoomReq;
import com.goorm.roomflow.domain.room.dto.response.*;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.mapper.MeetingRoomMapper;
import com.goorm.roomflow.domain.room.mapper.RoomSlotMapper;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotBulkRepository;
import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import com.goorm.roomflow.global.s3.S3ImageService;
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
import org.springframework.web.multipart.MultipartFile;

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
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomSlotService roomSlotService;
    private final S3ImageService s3ImageService;

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

    /**
     * 관리자 기능 구현
     */

    /**
     * 관리자 회의실 조회
     */
    @Override
    @Transactional
    public List<MeetingRoomAdminRes> readMeetingRoomAdminList() {

        log.info("회의실 목록 조회 시작");
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();

        log.info("회의실 목록 조회 완료: roomsCount={}", rooms.size());
        return rooms.stream()
                .map(room -> new MeetingRoomAdminRes(
                        room.getRoomId(),
                        room.getRoomName(),
                        room.getCapacity(),
                        room.getDescription(),
                        room.getHourlyPrice(),
                        room.getStatus(),
                        room.getImageUrl(),
                        room.getTotalReservations(),
                        room.getCreatedAt(),
                        room.getUpdatedAt()
                ))
                .sorted(
                        Comparator.comparingInt((MeetingRoomAdminRes room) -> room.status().getPriority())
                                .thenComparing(Comparator.comparingInt(MeetingRoomAdminRes::totalReservations).reversed())
                )
                .toList();
    }

    /**
     * 회의실 생성
     * @param meetingRoomReq
     */
    @Override
    @Transactional
    public void createMeetingRoom(MeetingRoomReq meetingRoomReq, MultipartFile imageFile) {
        log.info("회의실 생성 시작 - roomName={}", meetingRoomReq.roomName());

        String imageUrl = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3ImageService.upload(imageFile, "room");
        }

        MeetingRoom meetingRoom = MeetingRoom.builder()
                .roomName(meetingRoomReq.roomName())
                .capacity(meetingRoomReq.capacity())
                .description(meetingRoomReq.description())
                .hourlyPrice(meetingRoomReq.hourlyPrice())
                .status(meetingRoomReq.status())
                .imageUrl(imageUrl)
                .build();

        MeetingRoom savedRoom = meetingRoomRepository.save(meetingRoom);

        roomSlotService.createInitialSlotsForRoom(savedRoom);

        log.info("회의실 생성 완료 - roomId={}", savedRoom.getRoomId());
    }

    @Transactional
    public void modifyMeetingRoom(Long roomId, MeetingRoomReq meetingRoomReq, MultipartFile imageFile) {
        log.info("회의실 수정 시작 - roomId={}", roomId);

        MeetingRoom meetingRoom = loadMeetingRoom(roomId);

        String imageUrl = meetingRoom.getImageUrl();

        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3ImageService.upload(imageFile, "room");
        }

        meetingRoom.updateRoomInfo(
                meetingRoomReq.roomName(),
                meetingRoomReq.capacity(),
                meetingRoomReq.description(),
                meetingRoomReq.hourlyPrice(),
                meetingRoomReq.status(),
                imageUrl
        );

        log.info("회의실 수정 완료 - roomId={}", roomId);
    }

    @Transactional
    public void changeMeetingRoomStatus(Long roomId, RoomStatus targetStatus) {
        log.info("회의실 상태 변경 요청 시작 - roomId={}, targetStatus={}", roomId, targetStatus);

        MeetingRoom meetingRoom = loadMeetingRoom(roomId);

        RoomStatus currentStatus = meetingRoom.getStatus();

        log.info("현재 회의실 상태 확인 - roomId={}, currentStatus={}", roomId, currentStatus);

        if (currentStatus == targetStatus) {
            log.info("회의실 상태 변경 생략 - 동일한 상태 요청 - roomId={}, status={}", roomId, targetStatus);
            return;
        }

        if (targetStatus == RoomStatus.INACTIVE) {
            log.info("회의실 INACTIVE 전환 검증 시작 - roomId={}", roomId);
            changeToInactive(meetingRoom);
            return;
        }

        meetingRoom.updateStatus(targetStatus);

        log.info("회의실 상태 변경 완료 - roomId={}, from={}, to={}", roomId, currentStatus, targetStatus);
    }

    private void changeToInactive(MeetingRoom meetingRoom) {

        Long roomId = meetingRoom.getRoomId();
        LocalDateTime now = LocalDateTime.now();

        log.info("회의실 INACTIVE 변경 검증 시작 - roomId={}, 기준시간={}", roomId, now);

        boolean hasFutureReservation =
                reservationRoomRepository.existsFutureReservationByRoomId(roomId, now);

        log.info("미래 예약 존재 여부 확인 - roomId={}, hasFutureReservation={}", roomId, hasFutureReservation);

        if (hasFutureReservation) {
            log.info("회의실 INACTIVE 변경 실패 - 미래 예약 존재 - roomId={}", roomId);
            throw new BusinessException(ErrorCode.ROOM_STATUS_CHANGE_FORBIDDEN);
        }

        meetingRoom.updateStatus(RoomStatus.INACTIVE);

        log.info("회의실 상태 변경 완료 - roomId={}, to=INACTIVE", roomId);
    }

    private MeetingRoom loadMeetingRoom(Long roomId) {
        return meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }
}
