package com.goorm.roomflow.domain.reservation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ReservationPolicyKey {

    MAX_RESERVATION_HOURS("최대 예약 가능 시간", PolicyValueType.NUMBER, 1, 24, null),
    CANCEL_DEADLINE_MINUTES("예약 취소 가능 시간", PolicyValueType.NUMBER, 0, 1440, null),
    MAX_EQUIPMENT_PER_RESERVATION("예약당 최대 비품 수량", PolicyValueType.NUMBER, 1, 50, null),
    RESERVATION_START_TIME("예약 시작 가능 시간", PolicyValueType.TIME, null, null, null),
    RESERVATION_END_TIME("예약 종료 가능 시간", PolicyValueType.TIME, null, null, null),
    SLOT_UNIT_MINUTES("슬롯 단위 (분)", PolicyValueType.NUMBER, null, null, new String[]{"30", "60"}),
    OPEN_RESERVATION_DAYS_AHEAD("예약 가능 오픈 기간", PolicyValueType.NUMBER, 1, 365, null),
    MAINTENANCE_LIMIT("비품 점검 회차", PolicyValueType.NUMBER, 1, 1000, null);

    private final String displayName;
    private final PolicyValueType valueType;
    private final Integer min;
    private final Integer max;
    private final String[] allowedValues;

    public static ReservationPolicyKey from(String key) {
        return Arrays.stream(values())
                .filter(value -> value.name().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 정책 키입니다."));
    }

    public boolean hasAllowedValues() {
        return allowedValues != null && allowedValues.length > 0;
    }

    public boolean isAllowedValue(String value) {
        if (!hasAllowedValues()) {
            return true;
        }

        return Arrays.stream(allowedValues)
                .anyMatch(allowed -> allowed.equals(value));
    }
}
