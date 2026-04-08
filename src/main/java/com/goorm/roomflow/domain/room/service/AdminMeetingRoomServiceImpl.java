package com.goorm.roomflow.domain.room.service;

import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.dto.request.AdminMeetingRoomReq;
import com.goorm.roomflow.domain.room.dto.response.AdminMeetingRoomRes;
import com.goorm.roomflow.domain.room.entity.MeetingRoom;
import com.goorm.roomflow.domain.room.entity.RoomStatus;
import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import com.goorm.roomflow.global.s3.S3ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMeetingRoomServiceImpl implements AdminMeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomSlotService roomSlotService;
    private final S3ImageService s3ImageService;

    /**
     * 관리자 회의실 조회
     */
    @Transactional
    @Override
    public List<AdminMeetingRoomRes> readMeetingRoomAdminList() {

        log.info("회의실 목록 조회 시작");
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();

        log.info("회의실 목록 조회 완료: roomsCount={}", rooms.size());
        return rooms.stream()
                .map(room -> new AdminMeetingRoomRes(
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
                        Comparator.comparingInt((AdminMeetingRoomRes room) -> room.status().getPriority())
                                .thenComparing(Comparator.comparingInt(AdminMeetingRoomRes::totalReservations).reversed())
                )
                .toList();
    }

    /**
     * 회의실 생성
     * @param adminMeetingRoomReq
     */
    @Transactional
    @Override
    public void createMeetingRoom(AdminMeetingRoomReq adminMeetingRoomReq) {
        log.info("회의실 생성 시작 - roomName={}", adminMeetingRoomReq.roomName());

        String imageUrl = null;

        if (adminMeetingRoomReq.imageFile() != null && !adminMeetingRoomReq.imageFile().isEmpty()) {
            imageUrl = s3ImageService.upload(adminMeetingRoomReq.imageFile(), "room");
        }

        MeetingRoom meetingRoom = new MeetingRoom(
                adminMeetingRoomReq.roomName(),
                adminMeetingRoomReq.capacity(),
                adminMeetingRoomReq.description(),
                adminMeetingRoomReq.hourlyPrice(),
                adminMeetingRoomReq.status(),
                imageUrl
        );

        MeetingRoom savedRoom = meetingRoomRepository.save(meetingRoom);

        roomSlotService.createInitialSlotsForRoom(savedRoom);

        log.info("회의실 생성 완료 - roomId={}", savedRoom.getRoomId());
    }

    @Transactional
    @Override
    public void modifyMeetingRoom(Long roomId, AdminMeetingRoomReq adminMeetingRoomReq) {
        log.info("회의실 수정 시작 - roomId={}", roomId);

        MeetingRoom meetingRoom = loadMeetingRoom(roomId);

        String imageUrl = meetingRoom.getImageUrl();

        if (adminMeetingRoomReq.imageFile() != null && !adminMeetingRoomReq.imageFile().isEmpty()) {
            imageUrl = s3ImageService.upload(adminMeetingRoomReq.imageFile(), "room");
        }

        meetingRoom.updateRoomInfo(
                adminMeetingRoomReq.roomName(),
                adminMeetingRoomReq.capacity(),
                adminMeetingRoomReq.description(),
                adminMeetingRoomReq.hourlyPrice(),
                adminMeetingRoomReq.status(),
                imageUrl
        );

        log.info("회의실 수정 완료 - roomId={}", roomId);
    }

    @Transactional
    @Override
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
