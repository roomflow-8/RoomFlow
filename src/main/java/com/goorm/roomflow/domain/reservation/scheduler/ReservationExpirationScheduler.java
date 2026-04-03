package com.goorm.roomflow.domain.reservation.scheduler;

import com.goorm.roomflow.domain.reservation.entity.*;
import com.goorm.roomflow.domain.reservation.event.ReservationStatusChangedEvent;
import com.goorm.roomflow.domain.reservation.repository.ReservationEquipmentRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.reservation.repository.ReservationRoomRepository;
import com.goorm.roomflow.domain.room.entity.RoomSlot;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private static final long PENDING_MINUTES = 10L;
    private static final Long SYSTEM_USER_ID = 1L;
    private static final String EXPIRE_REASON = "AUTO_EXPIRED";

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final ReservationEquipmentRepository reservationEquipmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserJpaRepository userRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 일정 시간 이상 결제되지 않은 PENDING 예약 자동 만료 처리
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePendingReservations() {

        if (!running.compareAndSet(false, true)) {
            log.warn("이전 작업 실행 중");
            return;
        }

        try {

            // 만료 기준 시간 계산 (현재 시간 - 정책 시간)
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_MINUTES);

            log.info("[ReservationExpire] PENDING 예약 만료 처리 시작 - threshold={}", threshold);

            User admin = userRepository.findById(SYSTEM_USER_ID).get();

            // Pending 대상 조회
            List<Reservation> expiredReservations =
                    reservationRepository.findExpiredPendingReservations(
                            ReservationStatus.PENDING,
                            threshold
                    );

            if (expiredReservations.isEmpty()) {
                log.info("[ReservationExpire] PENDING 예약 만료 처리 완료 - 만료 대상 없음");
                return;
            }

            log.info("[ReservationExpire] 만료 대상 예약 수 = {}", expiredReservations.size());

            for (Reservation reservation : expiredReservations) {
                reservation.expire(EXPIRE_REASON);

                List<ReservationRoom> reservationRooms = reservation.getReservationRooms();

                // 슬롯 복구
                for (ReservationRoom reservationRoom : reservationRooms) {
                    reservationRoom.getRoomSlot().updateActiveStatus(true);
                }

                // 연결 엔티티 삭제
                reservationRoomRepository.deleteAll(reservationRooms);

                eventPublisher.publishEvent(new ReservationStatusChangedEvent(
                        reservation,
                        TargetType.RESERVATION,
                        reservation.getReservationId(),
                        ReservationStatus.PENDING,
                        reservation.getStatus(),
                        admin,
                        reservation.getCancelReason()
                ));

                log.info("회의실 예약 상태 변경 이벤트 발행 - reservationId={}, {} -> {}", reservation.getReservationId(), ReservationStatus.PENDING, reservation.getStatus());


            }

            log.info("[ReservationExpire] PENDING 예약 만료 처리 완료 - totalExpired={}", expiredReservations.size());
        } finally {
            running.set(false);
        }
    }


    /**
     * PENDING 상태 비품 예약 자동 만료
     * - 매 1분마다 실행
     * - PENDING 상태이고 10분 이상 경과한 비품 예약을 EXPIRED로 변경
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expirePendingEquipments() {
        log.info("====== 비품 예약 자동 만료 스케줄러 시작 ======");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_MINUTES);

        // 만료 대상 조회
        List<ReservationEquipment> expiredEquipments =
                reservationEquipmentRepository.findExpiredPendingEquipments(
                        ReservationStatus.PENDING,
                        threshold
                );

        if (expiredEquipments.isEmpty()) {
            log.info("만료 대상 없음");
            log.info("====== 비품 예약 자동 만료 스케줄러 종료 ======");
            return;
        }

        // 만료 대상 ID 목록
        List<Long> equipmentIds = expiredEquipments.stream()
                .map(ReservationEquipment::getReservationEquipmentId)
                .toList();

        log.info("만료 대상 비품 예약 발견 - {}건, IDs: {}",
                expiredEquipments.size(),
                equipmentIds);

        String reason = "시간 초과로 자동 만료";

        // 각 비품 예약 만료 처리
        expiredEquipments.forEach(equipment -> {
            try {
                ReservationStatus fromStatus = equipment.getStatus();

                equipment.expire(reason);

                log.info("비품 예약 자동 만료 - equipmentId: {}, reservationId: {}",
                        equipment.getReservationEquipmentId(),
                        equipment.getReservation().getReservationId());

                // 이벤트 발행
                publishStatusChangedEvent(equipment, fromStatus, null, reason);

            } catch (Exception e) {
                log.error("비품 예약 만료 실패 - equipmentId: {}",
                        equipment.getReservationEquipmentId(), e);
            }
        });

        log.info("비품 예약 만료 처리 완료 - 처리: {}건, IDs: {}",
                expiredEquipments.size(),
                equipmentIds);
        log.info("====== 비품 예약 자동 만료 스케줄러 종료 ======");
    }


    /**
     * 상태 변경 이벤트 발행
     */
    private void publishStatusChangedEvent(
            ReservationEquipment equipment,
            ReservationStatus fromStatus,
            User changedBy,
            String reason
    ) {
        if (fromStatus == equipment.getStatus()) {
            return;
        }

        eventPublisher.publishEvent(new ReservationStatusChangedEvent(
                equipment.getReservation(),
                TargetType.EQUIPMENT,
                equipment.getReservationEquipmentId(),
                fromStatus,
                equipment.getStatus(),
                changedBy,  // null 가능 (시스템 자동 처리)
                reason
        ));
    }
}






