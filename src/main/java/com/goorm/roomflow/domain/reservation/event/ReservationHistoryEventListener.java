package com.goorm.roomflow.domain.reservation.event;

import com.goorm.roomflow.domain.reservation.entity.ReservationHistory;
import com.goorm.roomflow.domain.reservation.repository.ReservationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationHistoryEventListener {

    private final ReservationHistoryRepository reservationHistoryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReservationStatusChanged(ReservationStatusChangedEvent event) {
        if (event.fromStatus() == event.toStatus()) {
            return;
        }

        ReservationHistory history = ReservationHistory.builder()
                .reservation(event.reservation())
                .targetType(event.targetType())
                .targetId(event.targetId())
                .fromStatus(event.fromStatus())
                .toStatus(event.toStatus())
                .changedBy(event.changedBy())
                .reason(event.reason())
                .build();

        reservationHistoryRepository.save(history);
    }
}
