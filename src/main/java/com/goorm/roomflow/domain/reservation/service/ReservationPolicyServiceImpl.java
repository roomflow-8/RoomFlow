package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationPolicyServiceImpl implements ReservationPolicyService {

    private final ReservationPolicyRepository reservationPolicyRepository;

    public int getIntValue(String key) {
        ReservationPolicy policy = reservationPolicyRepository.findByPolicyKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        return Integer.parseInt(policy.getPolicyValue());
    }

    public LocalTime getTimeValue(String key) {
        ReservationPolicy policy = reservationPolicyRepository.findByPolicyKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        return LocalTime.parse(policy.getPolicyValue());
    }

    public String getStringValue(String key) {
        ReservationPolicy policy = reservationPolicyRepository.findByPolicyKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        return policy.getPolicyValue();
    }
}
