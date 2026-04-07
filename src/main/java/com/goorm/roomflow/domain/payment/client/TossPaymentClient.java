package com.goorm.roomflow.domain.payment.client;

import com.goorm.roomflow.domain.payment.dto.response.TossErrorRes;
import com.goorm.roomflow.domain.payment.dto.response.TossPaymentRes;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class TossPaymentClient {

	private final WebClient webClient;
	private final String encodedSecretKey;

	public TossPaymentClient(WebClient webClient,
							 @Value("${payment.toss.secret-key}") String secretKey) {
		this.webClient = webClient;
		this.encodedSecretKey = Base64.getEncoder()
				.encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 결제 승인 API 호출
	 */
	public TossPaymentRes confirmPayment(String paymentKey, String orderId, Long amount) {
		Map<String, Object> body = Map.of(
				"paymentKey", paymentKey,
				"orderId", orderId,
				"amount", amount
		);

		try {
			log.info("토스 결제 승인 요청 - orderId={}, amount={}", orderId, amount);

			TossPaymentRes response = webClient.post()
					.uri("/confirm")
					.header("Authorization", "Basic " + encodedSecretKey)
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(body)
					.retrieve()
					.onStatus(status -> status.is4xxClientError(), clientResponse ->
							clientResponse.bodyToMono(TossErrorRes.class)  // ← 여기서 사용!
									.flatMap(error -> {
										log.error("토스 4xx 에러 - code={}, message={}", error.code(), error.message());
										return Mono.error(mapToBusinessException(error.code()));
									})
					)
					.onStatus(status -> status.is5xxServerError(), clientResponse -> {
						log.error("토스 서버 에러 - orderId={}", orderId);
						return Mono.error(new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED));
					})
					.bodyToMono(TossPaymentRes.class)
					.block();

			log.info("토스 결제 승인 성공 - orderId={}, paymentKey={}", orderId, paymentKey);
			return response;

		} catch (WebClientResponseException e) {
			log.error("토스 결제 승인 실패 - orderId={}, status={}, body={}",
					orderId, e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		}
	}

	/**
	 * 결제 취소 API 호출
	 */
	public TossPaymentRes cancelPayment(String paymentKey, Long cancelAmount, String cancelReason) {
		Map<String, Object> body = Map.of(
				"cancelReason", cancelReason,
				"cancelAmount", cancelAmount
		);

		try {
			log.info("토스 결제 취소 요청 - paymentKey={}, cancelAmount={}", paymentKey, cancelAmount);

			TossPaymentRes response = webClient.post()
					.uri("/" + paymentKey + "/cancel")
					.header("Authorization", "Basic " + encodedSecretKey)
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(body)
					.retrieve()
					.onStatus(status -> status.is4xxClientError(), clientResponse ->
							clientResponse.bodyToMono(TossErrorRes.class)
									.flatMap(error -> {
										log.error("토스 취소 4xx 에러 - code={}, message={}", error.code(), error.message());
										return Mono.error(mapToCancelException(error.code()));
									})
					)
					.onStatus(status -> status.is5xxServerError(), clientResponse -> {
						log.error("토스 서버 에러 - paymentKey={}", paymentKey);
						return Mono.error(new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED));
					})
					.bodyToMono(TossPaymentRes.class)
					.block();

			log.info("토스 결제 취소 성공 - paymentKey={}", paymentKey);
			return response;

		} catch (WebClientResponseException e) {
			log.error("토스 결제 취소 실패 - paymentKey={}, status={}, body={}",
					paymentKey, e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
		}
	}

	private BusinessException mapToBusinessException(String tossErrorCode) {
		return switch (tossErrorCode) {
			case "PAY_PROCESS_CANCELED" -> new BusinessException(ErrorCode.PAY_PROCESS_CANCELED);
			case "PAY_PROCESS_ABORTED" -> new BusinessException(ErrorCode.PAY_PROCESS_ABORTED);
			case "REJECT_CARD_COMPANY" -> new BusinessException(ErrorCode.REJECT_CARD_COMPANY);
			case "UNAUTHORIZED_KEY" -> new BusinessException(ErrorCode.UNAUTHORIZED_KEY);
			case "NOT_FOUND_PAYMENT_SESSION" -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT_SESSION);
			case "FORBIDDEN_REQUEST" -> new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
			default -> new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
		};

	}

	/**
	 * 토스 에러 코드 → 결제 취소 BusinessException 매핑
	 */
	private BusinessException mapToCancelException(String tossErrorCode) {
		return switch (tossErrorCode) {
			case "UNAUTHORIZED_KEY" -> new BusinessException(ErrorCode.UNAUTHORIZED_KEY);
			case "FORBIDDEN_REQUEST" -> new BusinessException(ErrorCode.FORBIDDEN_REQUEST);
			case "NOT_FOUND_PAYMENT" -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
			default -> new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
		};
	}
}