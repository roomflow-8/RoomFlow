//package com.goorm.roomflow.domain.room.service;
//
//import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
//import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
//import com.goorm.roomflow.domain.room.entity.MeetingRoom;
//import com.goorm.roomflow.domain.room.entity.RoomStatus;
//import com.goorm.roomflow.domain.room.mapper.MeetingRoomMapper;
//import com.goorm.roomflow.domain.room.mapper.RoomSlotMapper;
//import com.goorm.roomflow.domain.room.repository.MeetingRoomRepository;
//import com.goorm.roomflow.domain.room.repository.RoomSlotBulkRepository;
//import com.goorm.roomflow.domain.room.repository.RoomSlotRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.system.CapturedOutput;
//import org.springframework.boot.test.system.OutputCaptureExtension;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.dao.CannotAcquireLockException;
//import org.springframework.retry.annotation.EnableRetry;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//
///*
//* retry 테스트 코드
//* */
//
//@ExtendWith(OutputCaptureExtension.class)
//@SpringBootTest(
//        classes = {
//                MeetingRoomServiceImpl.class,
//                MeetingRoomServiceImplTest.TestConfig.class
//        }
//)
//class MeetingRoomServiceImplTest {
//
//    @Configuration
//    @EnableRetry
//    static class TestConfig {
//    }
//
//    @Autowired
//    private MeetingRoomService meetingRoomService;
//
//    @MockitoBean
//    private MeetingRoomRepository meetingRoomRepository;
//
//    @MockitoBean
//    private RoomSlotRepository roomSlotRepository;
//
//    @MockitoBean
//    private RoomSlotBulkRepository roomSlotBulkRepository;
//
//    @MockitoBean
//    private ReservationPolicyRepository reservationPolicyRepository;
//
//    @MockitoBean
//    private MeetingRoomMapper meetingRoomMapper;
//
//    @MockitoBean
//    private RoomSlotMapper roomSlotMapper;
//
//    /**
//     * 두 번 실패 후 세 번째 성공
//     */
//    @Test
//    void 슬롯생성_두번실패후_세번째성공() {
//        LocalDate targetDate = LocalDate.of(2026, 4, 30);
//
//        MeetingRoom room = MeetingRoom.builder()
//                .roomName("A회의실")
//                .capacity(6)
//                .hourlyPrice(BigDecimal.valueOf(10000))
//                .status(RoomStatus.AVAILABLE)
//                .build();
//
//        doThrow(new CannotAcquireLockException("1차 실패"))
//                .doThrow(new CannotAcquireLockException("2차 실패"))
//                .doReturn(List.of(room))
//                .when(meetingRoomRepository)
//                .findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);
//
//        doReturn(List.of(
//                ReservationPolicy.builder()
//                        .policyKey("RESERVATION_START_TIME")
//                        .policyValue("09:00")
//                        .build(),
//                ReservationPolicy.builder()
//                        .policyKey("RESERVATION_END_TIME")
//                        .policyValue("18:00")
//                        .build(),
//                ReservationPolicy.builder()
//                        .policyKey("SLOT_UNIT_MINUTES")
//                        .policyValue("30")
//                        .build()
//        )).when(reservationPolicyRepository).findByPolicyKeyIn(anyList());
//
//        meetingRoomService.generateSlots(targetDate);
//
//        verify(meetingRoomRepository, times(3))
//                .findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);
//    }
//
//    /**
//     * 세 번 모두 실패하면 recover 실행
//     */
//    @Test
//    void 슬롯생성_세번실패하면_recover가_실행된다(CapturedOutput output) {
//        LocalDate targetDate = LocalDate.of(2026, 4, 30);
//
//        doThrow(new CannotAcquireLockException("계속 실패"))
//                .when(meetingRoomRepository)
//                .findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);
//
//        meetingRoomService.generateSlots(targetDate);
//
//        verify(meetingRoomRepository, times(3))
//                .findByStatusIn(RoomStatus.AVAILABLE, RoomStatus.MAINTENANCE);
//
//        assert output.getOut().contains("전체 회의실 슬롯 생성 최종 실패");
//    }
//}