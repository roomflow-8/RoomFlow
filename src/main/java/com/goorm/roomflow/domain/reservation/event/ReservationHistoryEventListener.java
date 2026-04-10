package com.goorm.roomflow.domain.reservation.event;

import com.goorm.roomflow.domain.reservation.entity.ReservationHistory;
import com.goorm.roomflow.domain.reservation.repository.ReservationHistoryRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationHistoryEventListener {

    private final ReservationHistoryRepository reservationHistoryRepository;

    @Async("reservationHistoryExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReservationStatusChanged(ReservationStatusChangedEvent event) {

        log.info("이벤트 수신 - targetType: {}, targetId: {}, from: {} -> to: {}",
                event.targetType(), event.targetId(), event.fromStatus(), event.toStatus());


        if (event.fromStatus() == event.toStatus()) {
            return;
        }

        try {

            ReservationHistory history = ReservationHistory.builder()
                    .reservation(event.reservation())
                    .targetType(event.targetType())
                    .targetId(event.targetId())
                    .fromStatus(event.fromStatus())
                    .toStatus(event.toStatus())
                    .changedBy(event.changedBy())
                    .reason(event.reason())
                    .build();

            ReservationHistory saved = reservationHistoryRepository.save(history);
            log.info("히스토리 저장 완료 - historyId: {}, targetType: {}",
                    saved.getHistoryId(), saved.getTargetType());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
