package com.goorm.roomflow.domain.payment.service;

import com.goorm.roomflow.domain.payment.client.TossPaymentClient;
import com.goorm.roomflow.domain.payment.dto.request.PaymentCheckoutReq;
import com.goorm.roomflow.domain.payment.dto.response.PaymentResultRes;
import com.goorm.roomflow.domain.payment.dto.response.TossPaymentRes;
import com.goorm.roomflow.domain.payment.entity.Payment;
import com.goorm.roomflow.domain.payment.entity.PaymentStatus;
import com.goorm.roomflow.domain.payment.repository.PaymentRepository;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationRoomRes;
import com.goorm.roomflow.domain.reservation.dto.response.ReservationTimeSlot;
import com.goorm.roomflow.domain.reservation.entity.Reservation;
import com.goorm.roomflow.domain.reservation.repository.ReservationRepository;
import com.goorm.roomflow.domain.user.entity.User;
import com.goorm.roomflow.domain.user.repository.UserJpaRepository;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	// 결제 승인 과정에는 secret key가 필요
	@Value("${payment.toss.secret-key}")
	private String secretKey;

	private final PaymentRepository paymentRepository;
	private final ReservationRepository reservationRepository;
	private final UserJpaRepository userRepository;
	private final TossPaymentClient tossPaymentClient;

	/**
	 * 결제 체크아웃 정보 생성
	 */
	public PaymentCheckoutReq createCheckoutInfo(ReservationRoomRes reservationRoom) {

		Long amount = reservationRoom.totalAmount().longValue();
		String orderId = generateOrderId(reservationRoom.reservationId());
		String orderName = generateOrderName(reservationRoom);

		log.info("결제 정보 생성 - orderId={}, amount={}, orderName={}", orderId, amount, orderName);

		return PaymentCheckoutReq.of(
				amount,
				orderId,
				orderName,
				reservationRoom.reservationId(),
				reservationRoom.roomAmount(),
				reservationRoom.equipmentAmount()
		);
	}

	private String generateOrderId(Long reservationId) {
		String orderId = UUID.randomUUID().toString().substring(0, 12);

//		return "ROOMFLOW-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
//				+ "-" + reservationId;
		return reservationId + orderId;
	}

	private String generateOrderName(ReservationRoomRes reservationRoom) {
		String roomName = reservationRoom.roomName();
		String date = reservationRoom.reservationDate()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		// 시간 슬롯들 합치기
		String timeRanges = reservationRoom.reservationTimeSlots().stream()
				.map(ReservationTimeSlot::timeRange)
				.collect(Collectors.joining(", "));

		String roomPart = roomName + " " + date + " " + timeRanges;

		// 비품이 있으면 추가
		if (reservationRoom.equipments() != null && !reservationRoom.equipments().isEmpty()) {
			int equipmentCount = reservationRoom.equipments().size();
			return roomPart + ", 비품 " + equipmentCount + "건";
		}

		return roomPart;
	}

	@Override
	@Transactional
	public void savePaymentRequest(Long userId, Long reservationId,
								   String orderId, String orderName,
								   BigDecimal roomAmount, BigDecimal equipmentAmount) {


		// 이미 해당 예약에 대한 READY 또는 DONE 결제가 있는지 확인
		Optional<Payment> existingPayment = paymentRepository
				.findByReservation_ReservationIdAndStatusIn(
						reservationId,
						List.of(PaymentStatus.READY, PaymentStatus.DONE)
				);

		if (existingPayment.isPresent()) {
			Payment payment = existingPayment.get();

			if (payment.getStatus() == PaymentStatus.DONE) {
				log.warn("이미 결제 완료된 예약 - reservationId={}", reservationId);
				throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
			}

			// READY 상태면 기존 결제 정보 사용 (새로 생성 안 함)
			log.info("기존 결제 요청 사용 - orderId={}", payment.getOrderId());
			return;
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

		Payment payment = Payment.create(user, reservation, orderId, orderName, roomAmount, equipmentAmount);
		paymentRepository.save(payment);

		log.info("결제 요청 저장 - orderId={}, totalAmount={}", orderId, payment.getTotalAmount());
	}


	@Override
	@Transactional
	public PaymentResultRes confirmPayment(String orderId, String paymentKey, Long amount) {

		log.info("결제 승인 처리 시작 - orderId={}, paymentKey={}, amount={}", orderId, paymentKey, amount);

		// 1. 저장된 결제 정보 조회
		Payment payment = paymentRepository.findByOrderId(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

		// 2. 금액 검증
		if (payment.getTotalAmount().longValue() != amount) {
			log.error("결제 금액 불일치 - orderId={}, expected={}, actual={}",
					orderId, payment.getTotalAmount(), amount);
			throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
		}

		// 3. 상태 검증
		if (payment.getStatus() != PaymentStatus.READY) {
			log.error("잘못된 결제 상태 - orderId={}, status={}", orderId, payment.getStatus());
			throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS);
		}

		// 4. 토스페이먼츠 결제 승인 API 호출
		TossPaymentRes tossResponse = tossPaymentClient.confirmPayment(paymentKey, orderId, amount);

		// 5. 결제 승인 처리
		String receiptUrl = tossResponse.receipt() != null
				? tossResponse.receipt().url() : null;

		payment.approve(paymentKey, tossResponse.method(), receiptUrl);

		log.info("결제 승인 완료 - orderId={}, paymentKey={}, method={}", orderId, paymentKey, tossResponse.method());

		return PaymentResultRes.from(payment);
	}

	@Override
	@Transactional
	public void failPayment(String orderId, String code, String message) {

		if (orderId == null) {
			// 구매자가 결제 취소한 경우 (PAY_PROCESS_CANCELED)
			log.info("결제 취소됨 (구매자) - code={}, message={}", code, message);
			return;
		}

		Payment payment = paymentRepository.findByOrderId(orderId)
				.orElse(null);

		if (payment != null && payment.getStatus() == PaymentStatus.READY) {
			payment.fail();
			log.info("결제 실패 처리 - orderId={}, code={}, message={}", orderId, code, message);
		}
	}


}


