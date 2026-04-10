package com.goorm.roomflow.global.exception;

import com.goorm.roomflow.domain.equipment.controller.EquipmentReservationRestController;
import com.goorm.roomflow.domain.reservation.controller.ReservationController;
import com.goorm.roomflow.domain.reservation.controller.ReservationRestController;
import com.goorm.roomflow.domain.room.controller.MeetingRoomRestController;
import com.goorm.roomflow.domain.user.controller.UserRestController;
import com.goorm.roomflow.global.code.ErrorCode;
import com.goorm.roomflow.global.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice(assignableTypes = {
		ReservationRestController.class,
		EquipmentReservationRestController.class,
		MeetingRoomRestController.class,
		UserRestController.class,
})
public class GlobalExceptionHandler {


	/**
	 * 비즈니스 로직에서 발생한 예외 처리
	 * - Service 계층에서 throw 되는 BusinessException을 처리
	 * - ErrorCode에 정의된 상태값과 메시지를 기반으로 응답 생성
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();

		log.warn("BusinessException 발생: code={}, message={}", errorCode.getCode(), errorCode.getMessage(), e);

		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode));
	}

	/**
	 * - @Valid 검증 실패 시 발생하는 예외 처리
	 * - RequestBody DTO의 필드 단위 검증 실패 (@NotNull, @Email 등)
	 * - 어떤 필드가 왜 실패했는지 ValidationError 리스트로 변환
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		List<ErrorResponse.ValidationError> errors = e.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toValidationError)
				.toList();

		return ResponseEntity
				.badRequest()
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, errors));
	}

	/**
	 * - @RequestParam, @PathVariable 검증 실패 시 발생하는 예외 처리
	 * - ConstraintViolationException 발생
	 * - (현재는 상세 필드 정보 없이 공통 에러로 처리)
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
		return ResponseEntity
				.badRequest()
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
	}

	/**
	 * 그 외 모든 예외 처리 (최종 fallback)
	 * - 예상하지 못한 예외를 처리하여 서버 에러로 응답
	 * - 내부 구현 정보는 노출하지 않음 (보안)
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {

		log.error(e.getMessage(), e);

		return ResponseEntity
				.status(ErrorCode.INTERNAL_ERROR.getStatus())
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
	}

	private ErrorResponse.ValidationError toValidationError(FieldError fieldError) {
		return ErrorResponse.ValidationError.of(
				fieldError.getField(),
				fieldError.getRejectedValue(),
				fieldError.getDefaultMessage()
		);
	}
}

