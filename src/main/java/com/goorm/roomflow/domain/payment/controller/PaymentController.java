package com.goorm.roomflow.domain.payment.controller;

import com.goorm.roomflow.domain.payment.dto.request.PaymentCheckoutReq;
import com.goorm.roomflow.domain.payment.dto.request.PaymentFailReq;
import com.goorm.roomflow.domain.payment.dto.request.PaymentSuccessReq;
import com.goorm.roomflow.domain.payment.dto.response.PaymentResultRes;
import com.goorm.roomflow.domain.payment.service.PaymentService;
import com.goorm.roomflow.domain.user.service.CustomUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/payment")
public class PaymentController {

	@Value("${payment.toss.client-key}")
	private String clientKey;

	@Value("${payment.toss.secret-key}")
	private String secretKey;

	private final PaymentService paymentService;

	@GetMapping("/checkout")
	public String checkout(@AuthenticationPrincipal CustomUser currentUser,
						   PaymentCheckoutReq checkoutReq,
						   Model model) {

		log.info("결제 페이지 진입 - userId={}, orderId={}", currentUser.getUserId(), checkoutReq.orderId());

		paymentService.savePaymentRequest(
				currentUser.getUserId(),
				checkoutReq.reservationId(),
				checkoutReq.orderId(),
				checkoutReq.orderName(),
				checkoutReq.roomAmount(),
				checkoutReq.equipmentAmount()
		);

		model.addAttribute("clientKey", clientKey);
		model.addAttribute("customerKey", "ROOMFLOW-USER-" + currentUser.getUserId());

		log.debug("amount={}, orderId={}, orderName={}", checkoutReq.amount(), checkoutReq.orderId(), checkoutReq.orderName());

		// 결제 정보
		model.addAttribute("amount", checkoutReq.amount());
		model.addAttribute("orderId", checkoutReq.orderId());
		model.addAttribute("orderName", checkoutReq.orderName());

		// 회원 정보는 현재 로그인한 사용자에서 가져옴
		model.addAttribute("customerEmail", currentUser.getEmail());
		model.addAttribute("customerName", currentUser.getName());

		return "payment/checkout";
	}


	@GetMapping("/success")
	public String success(
			PaymentSuccessReq successReq,
			Model model
	) {

		log.info("confirmPayment orderId={}", successReq.orderId());

		PaymentResultRes result = paymentService.confirmPayment(
				successReq.orderId(),
				successReq.paymentKey(),
				successReq.amount()
		);

		model.addAttribute("payment", result);

		return "payment/success";
	}


	/**
	 * 결제 실패 콜백
	 */
	@GetMapping("/fail")
	public String fail(
			PaymentFailReq failReq,
			Model model) {

		log.info("결제 실패 콜백 - code={}, message={}, orderId={}",
				failReq.code(), failReq.message(), failReq.orderId());

		paymentService.failPayment(failReq.orderId(), failReq.code(), failReq.message());

		model.addAttribute("code", failReq.code());
		model.addAttribute("message", failReq.message());
		model.addAttribute("orderId", failReq.orderId());

		return "payment/fail";
	}
}
