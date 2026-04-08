package com.goorm.roomflow.domain.reservation.service;

import com.goorm.roomflow.domain.reservation.dto.request.ReservationPolicyUpdateReq;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationPolicyAdminRes;

import java.util.List;

public interface AdminReservationPolicyService {

    List<ReservationPolicyAdminRes> getPolicyList();
    void updatePolicy(Long policyId, ReservationPolicyUpdateReq policyUpdateReq);
}
