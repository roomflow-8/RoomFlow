package com.goorm.roomflow.domain.payment.entity;

import com.goorm.roomflow.domain.BaseEntity;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	// 주문 정보
	@Column(nullable = false, unique = true, length = 64)
	private String orderId;

	@Column(nullable = false, length = 100)
	private String orderName;

	// 결제 정보
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(unique = true, length = 200)
	private String paymentKey;

	private LocalDateTime requestedAt;
	private LocalDateTime approvedAt;

	// 금액 정보
	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal roomAmount;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal equipmentAmount;

	@Column(nullable = false, precision = 10, scale = 0)
	private BigDecimal totalAmount;

	@Column(precision = 10, scale = 0)
	private BigDecimal balanceAmount;

	// 결제 수단
	@Column(length = 20)
	private String method;

	@Column(length = 500)
	private String receiptUrl;

	// 취소 내역
	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<PaymentCancel> cancels = new ArrayList<>();

	// 생성 메서드
	public static Payment create(User user, Reservation reservation,
								 String orderId, String orderName,
								 BigDecimal roomAmount, BigDecimal equipmentAmount) {
		BigDecimal total = roomAmount.add(equipmentAmount); // Total Amount 계산
		return Payment.builder()
				.user(user)
				.reservation(reservation)
				.orderId(orderId)
				.orderName(orderName)
				.status(PaymentStatus.READY)
				.roomAmount(roomAmount)
				.equipmentAmount(equipmentAmount)
				.totalAmount(total) // 계산된 값 입력
				.requestedAt(LocalDateTime.now())
				.build();
	}

	// 결제 승인
	public void approve(String paymentKey, String method, String receiptUrl) {
		this.status = PaymentStatus.DONE;
		this.paymentKey = paymentKey;
		this.method = method;
		this.receiptUrl = receiptUrl;
		this.approvedAt = LocalDateTime.now();
		this.balanceAmount = this.totalAmount;
	}

	// 결제 실패
	public void fail() {
		this.status = PaymentStatus.ABORTED;
	}

	// 취소 추가
	public void addCancel(PaymentCancel cancel) {
		this.cancels.add(cancel);
		cancel.setPayment(this);

		this.balanceAmount = this.balanceAmount.subtract(cancel.getCancelAmount());

		if (this.balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
			this.status = PaymentStatus.CANCELED;
		} else {
			this.status = PaymentStatus.PARTIAL_CANCELED;
		}
	}
}
