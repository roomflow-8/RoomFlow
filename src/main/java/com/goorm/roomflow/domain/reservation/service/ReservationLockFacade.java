package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.CreateReservationRoomReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.user.entity.User;
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

    public ReservationRoomRes createReservationRoom(Long userId, CreateReservationRoomReq request) {

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
            // 0초 대기, 트랜잭션 끝날 때까지 대기
            acquired = multiLock.tryLock(1, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new BusinessException(ErrorCode.ALREADY_PROCESSING_RESERVATION);
            }

            return reservationService.createReservationRoomTransactional(userId, request);

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
