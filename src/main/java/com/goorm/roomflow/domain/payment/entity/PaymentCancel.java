package com.goorm.roomflow.domain.payment.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.reservation.entity.ReservationEquipment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_cancels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentCancel extends BaseEntity {

//	@Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long cancelId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	// 취소 대상 (비품 취소인 경우)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_equipment_id")
	private ReservationEquipment reservationEquipment;

	// 토스페이먼츠 취소 정보
	@Column(length = 100)
	private String transactionKey;

	@Column(length = 100)
	private String receiptKey;

	// 취소 금액 정보
	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal cancelAmount;

	@Column(precision = 10, scale = 0)
	private BigDecimal taxFreeAmount;

	@Column(precision = 10, scale = 0)
	private BigDecimal refundableAmount;

	// 취소 상태
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentCancelStatus cancelStatus;

	// 취소 사유
	@Column(nullable = false, length = 255)
	private String cancelReason;

	// 취소 완료 시간 (토스에서 받은 시간)
	private LocalDateTime canceledAt;

	public void setPayment(Payment payment) {
		this.payment = payment;
	}

	// 생성 메서드
	public static PaymentCancel create(ReservationEquipment reservationEquipment,
									   BigDecimal cancelAmount,
									   String cancelReason) {
		return PaymentCancel.builder()
				.reservationEquipment(reservationEquipment)
				.cancelAmount(cancelAmount)
				.cancelReason(cancelReason)
				.cancelStatus(PaymentCancelStatus.PENDING)
				.build();
	}

	// 토스 응답으로 업데이트
	public void complete(String transactionKey, String receiptKey,
						 BigDecimal taxFreeAmount, BigDecimal refundableAmount,
						 LocalDateTime canceledAt) {
		this.transactionKey = transactionKey;
		this.receiptKey = receiptKey;
		this.taxFreeAmount = taxFreeAmount;
		this.refundableAmount = refundableAmount;
		this.canceledAt = canceledAt;
		this.cancelStatus = PaymentCancelStatus.DONE;
	}

	public void fail() {
		this.cancelStatus = PaymentCancelStatus.FAILED;
	}

}
