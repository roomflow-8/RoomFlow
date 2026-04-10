package com.goorm.roomflow.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReservationPolicy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long policyId;

	@Column(nullable = false, unique = true, length = 50)
	private String policyKey;

	@Column(length = 100)
	private String policyValue;

	@Column(length = 255)
	private String description;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public ReservationPolicy(String policyKey, String policyValue, String description) {
		this.policyKey = policyKey;
		this.policyValue = policyValue;
		this.description = description;
	}

	// --- 비즈니스 로직 (정책 값 변경) ---
	public void updateValue(String newValue) {
		this.policyValue = newValue;
	}
}