package com.goorm.roomflow.domain.reservation.repository;

import com.goorm.roomflow.domain.reservation.entity.ReservationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationPolicyRepository extends JpaRepository<ReservationPolicy, Long> {
    Optional<ReservationPolicy> findByPolicyKey(String policyKey);

    List<ReservationPolicy> findByPolicyKeyIn(List<String> policyKeys);
}
