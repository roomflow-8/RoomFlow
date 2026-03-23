package com.goorm.roomflow.domain.reservation.scheduler;

import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.entity.ReservationRoom;
import com.goorm.roomflow.domain.reservation.entity.ReservationStatus;
import com.goorm.roomflow.domain.reservation.entity.TargetType;
import com.goorm.roomflow.domain.reservation.event.ReservationStatusChangedEvent;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private static final long PENDING_MINUTES = 5L;

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserJpaRepository userRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePendingReservations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_MINUTES);

        User admin = userRepository.findById(3L).get();

        List<Reservation> expiredReservations =
                reservationRepository.findExpiredPendingReservations(
                        ReservationStatus.PENDING,
                        threshold
                );

        if (expiredReservations.isEmpty()) {
            return;
        }

        for (Reservation reservation : expiredReservations) {
            reservation.expire("Time Out");

            List<ReservationRoom> reservationRooms = reservationRoomRepository
                    .findByReservation(reservation);

            reservationRoomRepository.deleteAll(reservationRooms);

            for (ReservationRoom reservationRoom : reservationRooms) {
                reservationRoom.getRoomSlot().updateActiveStatus(true);
            }

            eventPublisher.publishEvent(new ReservationStatusChangedEvent(
                    reservation,
                    TargetType.RESERVATION,
                    reservation.getReservationId(),
                    ReservationStatus.PENDING,
                    reservation.getStatus(),
                    admin,
                    reservation.getCancelReason()
            ));
        }
    }
}
