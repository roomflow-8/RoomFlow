package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.AdminReservationPolicyUpdateReq;
import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationPolicyRes;
import com.goorm.roomflow.domain.reservation.entity.PolicyValueType;
import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
import com.goorm.roomflow.domain.reservation.entity.ReservationPolicyKey;
import com.goorm.roomflow.domain.reservation.repository.ReservationPolicyRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReservationPolicyServiceImpl implements AdminReservationPolicyService {

    private final ReservationPolicyRepository reservationPolicyRepository;


    @Override
    public List<AdminReservationPolicyRes> getPolicyList() {
        log.info("정책 관리 목록 조회");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return reservationPolicyRepository.findAllByOrderByPolicyIdAsc().stream()
                .map(policy -> {
                    ReservationPolicyKey key = ReservationPolicyKey.from(policy.getPolicyKey());

                    return new AdminReservationPolicyRes(
                            policy.getPolicyId(),
                            policy.getPolicyKey(),
                            policy.getPolicyValue(),
                            policy.getDescription(),
                            getPolicyGuide(key),
                            getInputType(key),
                            policy.getUpdatedAt().format(formatter)
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public void updatePolicy(Long policyId, AdminReservationPolicyUpdateReq policyUpdateReq) {

        log.info("정책 수정 시작 - policyId={}", policyId);

        ReservationPolicy policy = reservationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        String newValue = policyUpdateReq.policyValue();

        if (newValue == null || newValue.isBlank()) {
            throw new IllegalArgumentException("정책 값은 필수입니다.");
        }
        newValue = newValue.trim();

        validatePolicyValue(policy.getPolicyKey(), newValue);

        policy.updateValue(newValue);

        log.info("정책 수정 완료 - policyId={}", policy.getPolicyId());
    }

    private String getInputType(ReservationPolicyKey key) {
        return key.getValueType() == PolicyValueType.TIME ? "time" : "text";
    }

    private String getPolicyGuide(ReservationPolicyKey key) {
        if (key.getValueType() == PolicyValueType.TIME) {
            return "HH:mm 형식";
        }

        if (key.hasAllowedValues()) {
            return String.join(", ", key.getAllowedValues()) + "만 가능";
        }

        if (key.getMin() != null && key.getMax() != null) {
            return key.getMin() + " ~ " + key.getMax();
        }

        if (key.getMin() != null) {
            return key.getMin() + " 이상";
        }

        if (key.getMax() != null) {
            return key.getMax() + " 이하";
        }

        return "-";
    }

    private void validatePolicyValue(String policyKey, String policyValue) {
        ReservationPolicyKey key = ReservationPolicyKey.from(policyKey);

        switch (key.getValueType()) {
            case NUMBER -> validateNumberPolicy(key, policyValue);
            case TIME -> validateTimePolicy(policyValue);
        }

        validateTimeRange(policyKey, policyValue);
    }

    private void validateNumberPolicy(ReservationPolicyKey key, String policyValue) {
        int value;

        try {
            value = Integer.parseInt(policyValue);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.POLICY_INVALID_VALUE);
        }

        if (key.getMin() != null && value < key.getMin()) {
            throw new BusinessException(ErrorCode.POLICY_OUT_OF_RANGE);
        }

        if (key.getMax() != null && value > key.getMax()) {
            throw new BusinessException(ErrorCode.POLICY_OUT_OF_RANGE);
        }

        if (!key.isAllowedValue(policyValue)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_ALLOWED_VALUE);
        }
    }

    private void validateTimePolicy(String policyValue) {
        if (!policyValue.matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
            throw new BusinessException(ErrorCode.POLICY_INVALID_TIME_FORMAT);
        }
    }

    private void validateTimeRange(String policyKey, String policyValue) {
        if (!policyKey.equals("RESERVATION_START_TIME") && !policyKey.equals("RESERVATION_END_TIME")) {
            return;
        }

        String startTime = policyKey.equals("RESERVATION_START_TIME")
                ? policyValue
                : reservationPolicyRepository.findByPolicyKey("RESERVATION_START_TIME")
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND))
                .getPolicyValue();

        String endTime = policyKey.equals("RESERVATION_END_TIME")
                ? policyValue
                : reservationPolicyRepository.findByPolicyKey("RESERVATION_END_TIME")
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND))
                .getPolicyValue();

        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.POLICY_INVALID_TIME_FORMAT);
        }
    }
}
