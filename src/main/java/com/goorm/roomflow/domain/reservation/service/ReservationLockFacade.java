package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationLockFacade {

    private final RedissonClient redissonClient;
    private final ReservationService reservationService;

    public ReservationRoomRes createReservationRoom(CreateReservationRoomReq request) {

        List<String> lockKeys = request.roomSlotIds().stream()
                .sorted()
                .map(slotId -> "reservation:slot:" + slotId)
                .toList();

        RLock[] locks = lockKeys.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);

        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean acquired = false;

        try {
            // 최대 10초 대기, 락 획득 후 30초 뒤 자동 해제
            acquired = multiLock.tryLock(0, 30, TimeUnit.SECONDS);

            if (!acquired) {
                log.info("락 흭득 실패");
                throw new BusinessException(ErrorCode.ALREADY_PROCESSING_RESERVATION);
            }

            return reservationService.createReservationRoomTransactional(request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.ALREADY_PROCESSING_RESERVATION);
        } finally {
            if (acquired) {
                multiLock.unlock();
            }
        }
    }
}
