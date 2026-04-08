package com.goorm.roomflow.domain.reservation.dto.response;

public record AdminReservationPolicyRes(
        Long policyId,
        String policyKey,
        String policyValue,
        String description,
        String policyGuide,
        String inputType,
        String updatedAt
){
}
