package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.AdminReservationPolicyUpdateReq;
import com.goorm.roomflow.domain.reservation.dto.response.AdminReservationPolicyRes;

import java.util.List;

public interface AdminReservationPolicyService {

    List<AdminReservationPolicyRes> getPolicyList();
    void updatePolicy(Long policyId, AdminReservationPolicyUpdateReq policyUpdateReq);
}
